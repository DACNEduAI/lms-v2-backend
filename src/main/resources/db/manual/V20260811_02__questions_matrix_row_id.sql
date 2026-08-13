-- One-time, idempotent migration for databases created before assessment
-- variants stored their Exam Matrix row on each quiz question.
-- Run while the backend is stopped.
BEGIN;

CREATE TABLE IF NOT EXISTS edu.schema_migrations
(
    version    VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
DECLARE
    migration_version CONSTANT VARCHAR(100) := '20260811_02_questions_matrix_row_id';
BEGIN
    IF EXISTS (
        SELECT 1 FROM edu.schema_migrations WHERE version = migration_version
    ) THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'edu'
          AND table_name = 'questions'
          AND column_name = 'matrix_row_id'
    ) THEN
        ALTER TABLE edu.questions
            ADD COLUMN matrix_row_id UUID
                REFERENCES edu.exam_matrix_rows (id) ON DELETE SET NULL;
    END IF;

    INSERT INTO edu.schema_migrations (version) VALUES (migration_version);
END $$;

COMMIT;
