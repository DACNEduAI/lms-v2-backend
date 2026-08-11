-- One-time, idempotent migration for databases created before Quiz2 variants
-- stored their custom learning level on each quiz question.
-- Run while the backend is stopped.
BEGIN;

CREATE TABLE IF NOT EXISTS edu.schema_migrations
(
    version    VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
DECLARE
    migration_version CONSTANT VARCHAR(100) := '20260811_03_questions_custom_learning_level';
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
          AND column_name = 'custom_learning_level_id'
    ) THEN
        ALTER TABLE edu.questions
            ADD COLUMN custom_learning_level_id UUID
                REFERENCES edu.custom_learning_levels (id) ON DELETE SET NULL;
    END IF;

    -- Recover only metadata with an authoritative Question Bank source.
    -- Rows with no source or no recorded level remain intentionally NULL.
    UPDATE edu.questions AS question
    SET custom_learning_level_id = bank.custom_learning_level_id
    FROM edu.question_bank_questions AS bank
    WHERE question.custom_learning_level_id IS NULL
      AND question.source_bank_question_id = bank.id
      AND bank.custom_learning_level_id IS NOT NULL;

    INSERT INTO edu.schema_migrations (version) VALUES (migration_version);
END $$;

COMMIT;
