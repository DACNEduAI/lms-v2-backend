-- One-time, idempotent migration for databases created before material index
-- scheduling used absolute timestamps. Run while the backend is stopped.
BEGIN;

CREATE TABLE IF NOT EXISTS edu.schema_migrations
(
    version    VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
DECLARE
    migration_version CONSTANT VARCHAR(100) := '20260811_01_material_index_jobs_utc';
BEGIN
    IF EXISTS (
        SELECT 1 FROM edu.schema_migrations WHERE version = migration_version
    ) THEN
        RETURN;
    END IF;

    -- These columns were written by a JVM running in Asia/Ho_Chi_Minh.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'edu'
          AND table_name = 'material_index_jobs'
          AND column_name = 'next_attempt_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        EXECUTE $sql$
            ALTER TABLE edu.material_index_jobs
                ALTER COLUMN next_attempt_at TYPE TIMESTAMPTZ
                    USING next_attempt_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
                ALTER COLUMN lease_until TYPE TIMESTAMPTZ
                    USING lease_until AT TIME ZONE 'Asia/Ho_Chi_Minh',
                ALTER COLUMN completed_at TYPE TIMESTAMPTZ
                    USING completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh',
                ALTER COLUMN created_at TYPE TIMESTAMPTZ
                    USING created_at AT TIME ZONE 'UTC',
                ALTER COLUMN updated_at TYPE TIMESTAMPTZ
                    USING updated_at AT TIME ZONE 'UTC'
        $sql$;
    END IF;

    -- Active jobs for an older material revision must never ingest stale data.
    UPDATE edu.material_index_jobs AS job
    SET status = 'CANCELLED',
        lease_until = NULL,
        completed_at = NOW(),
        error_message = COALESCE(job.error_message, 'Material revision was superseded before UTC migration'),
        updated_at = NOW()
    WHERE job.operation = 'INGEST'
      AND job.status IN ('PENDING', 'PROCESSING')
      AND NOT EXISTS (
          SELECT 1
          FROM edu.materials AS material
          WHERE material.id = job.material_id
            AND material.course_id = job.course_id
            AND material.ingestion_revision = job.revision
      );

    -- Keep only the newest active job for the same material revision.
    WITH ranked AS (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY course_id, material_id, revision
                   ORDER BY created_at DESC, id DESC
               ) AS row_number
        FROM edu.material_index_jobs
        WHERE operation = 'INGEST'
          AND status IN ('PENDING', 'PROCESSING')
    )
    UPDATE edu.material_index_jobs AS job
    SET status = 'CANCELLED',
        lease_until = NULL,
        completed_at = NOW(),
        error_message = COALESCE(job.error_message, 'Duplicate active material index job cancelled by UTC migration'),
        updated_at = NOW()
    FROM ranked
    WHERE job.id = ranked.id
      AND ranked.row_number > 1;

    -- Resume each surviving current-revision job once. The durable recovery
    -- trigger will claim it after the backend starts.
    UPDATE edu.material_index_jobs AS job
    SET status = 'PENDING',
        next_attempt_at = NOW(),
        lease_until = NULL,
        error_message = NULL,
        completed_at = NULL,
        updated_at = NOW()
    WHERE job.operation = 'INGEST'
      AND job.status IN ('PENDING', 'PROCESSING')
      AND EXISTS (
          SELECT 1
          FROM edu.materials AS material
          WHERE material.id = job.material_id
            AND material.course_id = job.course_id
            AND material.ingestion_revision = job.revision
      );

    UPDATE edu.materials AS material
    SET ingestion_status = 'PENDING',
        ingestion_error = NULL,
        ingested_at = NULL,
        updated_at = NOW()
    WHERE material.status = 'UPLOADED'
      AND material.type = 'PDF'
      AND EXISTS (
          SELECT 1
          FROM edu.material_index_jobs AS job
          WHERE job.material_id = material.id
            AND job.course_id = material.course_id
            AND job.revision = material.ingestion_revision
            AND job.operation = 'INGEST'
            AND job.status = 'PENDING'
      );

    DROP INDEX IF EXISTS edu.uq_material_index_active_ingest_revision;
    CREATE UNIQUE INDEX uq_material_index_active_ingest_revision
        ON edu.material_index_jobs (course_id, material_id, revision)
        WHERE operation = 'INGEST' AND status IN ('PENDING', 'PROCESSING');

    INSERT INTO edu.schema_migrations (version) VALUES (migration_version);
END $$;

COMMIT;
