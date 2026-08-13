-- UTC instant normalization for the application schema.
--
-- Operational requirements:
--   1. Take a verified, restorable Supabase backup.
--   2. Stop LMS workers/Quartz and AI writers; no PENDING/PROCESSING jobs may remain.
--   3. Run this script exactly once against the target database.
--
-- Historical policy approved on 2026-08-14: existing timestamp-without-time-zone
-- values are literal UTC wall-clock values. Therefore 16:23 becomes 16:23Z; this
-- migration deliberately does not add or subtract seven hours.

CREATE TABLE IF NOT EXISTS edu.utc_timestamp_migration_audit (
    migration_id TEXT NOT NULL,
    table_name TEXT NOT NULL,
    column_name TEXT NOT NULL,
    row_count BIGINT NOT NULL,
    null_count BIGINT NOT NULL,
    min_value TIMESTAMPTZ,
    max_value TIMESTAMPTZ,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (migration_id, table_name, column_name)
);

DO $migration$
DECLARE
    item RECORD;
    rows BIGINT;
    nulls BIGINT;
    minimum TIMESTAMPTZ;
    maximum TIMESTAMPTZ;
BEGIN
    FOR item IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'edu'
          AND data_type = 'timestamp without time zone'
        ORDER BY table_name, ordinal_position
    LOOP
        EXECUTE format(
            'SELECT count(*), count(*) FILTER (WHERE %1$I IS NULL), '
            || 'min(%1$I AT TIME ZONE ''UTC''), max(%1$I AT TIME ZONE ''UTC'') '
            || 'FROM edu.%2$I',
            item.column_name, item.table_name
        ) INTO rows, nulls, minimum, maximum;

        INSERT INTO edu.utc_timestamp_migration_audit (
            migration_id, table_name, column_name, row_count, null_count, min_value, max_value
        ) VALUES (
            'V20260814_01', item.table_name, item.column_name, rows, nulls, minimum, maximum
        ) ON CONFLICT (migration_id, table_name, column_name) DO UPDATE
            SET row_count = EXCLUDED.row_count,
                null_count = EXCLUDED.null_count,
                min_value = EXCLUDED.min_value,
                max_value = EXCLUDED.max_value,
                captured_at = CURRENT_TIMESTAMP;

        EXECUTE format(
            'ALTER TABLE edu.%1$I ALTER COLUMN %2$I TYPE TIMESTAMPTZ '
            || 'USING %2$I AT TIME ZONE ''UTC''',
            item.table_name, item.column_name
        );
END LOOP;

    -- Keep the standard server-side default explicit and timezone-aware. Do
    -- not touch custom defaults: they require an application-specific review.
    FOR item IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'edu'
          AND data_type = 'timestamp with time zone'
          AND column_default IN ('CURRENT_TIMESTAMP', 'now()')
        ORDER BY table_name, ordinal_position
    LOOP
        EXECUTE format(
            'ALTER TABLE edu.%1$I ALTER COLUMN %2$I SET DEFAULT CURRENT_TIMESTAMP',
            item.table_name, item.column_name
        );
    END LOOP;
END
$migration$;

-- A deployment check must return zero before writers are restarted.
-- SELECT count(*) FROM information_schema.columns
-- WHERE table_schema = 'edu' AND data_type = 'timestamp without time zone';
