-- Preserve the browser-provided filename independently of the editable title.
-- Run this migration while the backend is stopped.
BEGIN;

CREATE TABLE IF NOT EXISTS edu.schema_migrations
(
    version    VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
DECLARE
    migration_version CONSTANT VARCHAR(100) := '20260917_01_material_original_file_name';
BEGIN
    IF EXISTS (SELECT 1 FROM edu.schema_migrations WHERE version = migration_version) THEN
        RETURN;
    END IF;

    ALTER TABLE edu.materials ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255);

    UPDATE edu.materials
    SET original_file_name = COALESCE(NULLIF(BTRIM(title), ''), 'material-' || id::text)
    WHERE original_file_name IS NULL OR BTRIM(original_file_name) = '';

    ALTER TABLE edu.materials ALTER COLUMN original_file_name SET NOT NULL;

    CREATE UNIQUE INDEX IF NOT EXISTS uq_material_uploaded_original_file_name_per_course
        ON edu.materials (course_id, LOWER(BTRIM(original_file_name)))
        WHERE status = 'UPLOADED';

    INSERT INTO edu.schema_migrations (version) VALUES (migration_version);
END $$;

COMMIT;
