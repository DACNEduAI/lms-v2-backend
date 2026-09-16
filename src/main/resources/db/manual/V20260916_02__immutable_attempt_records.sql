BEGIN;

DO $$
BEGIN
    IF to_regclass('edu.attempt_records') IS NULL
       AND to_regclass('edu.attempt_question_results') IS NOT NULL THEN
        ALTER TABLE edu.attempt_question_results RENAME TO attempt_records;
    END IF;
END $$;

ALTER TABLE edu.attempt_records
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS question_content TEXT,
    ADD COLUMN IF NOT EXISTS question_content_format VARCHAR(40),
    ADD COLUMN IF NOT EXISTS student_answer_number BIGINT,
    ADD COLUMN IF NOT EXISTS student_answer_position INTEGER,
    ADD COLUMN IF NOT EXISTS student_answer_content TEXT,
    ADD COLUMN IF NOT EXISTS student_answer_content_format VARCHAR(40),
    ADD COLUMN IF NOT EXISTS correct_answer_number BIGINT,
    ADD COLUMN IF NOT EXISTS correct_answer_position INTEGER,
    ADD COLUMN IF NOT EXISTS correct_answer_content TEXT,
    ADD COLUMN IF NOT EXISTS correct_answer_content_format VARCHAR(40),
    ADD COLUMN IF NOT EXISTS snapshot_complete BOOLEAN NOT NULL DEFAULT FALSE;

-- Legacy pending Code Runner rows did not have a final correctness value. They
-- are retained as incomplete snapshots and become permanently incorrect.
UPDATE edu.attempt_records SET is_correct = FALSE WHERE is_correct IS NULL;
ALTER TABLE edu.attempt_records ALTER COLUMN is_correct SET NOT NULL;

ALTER TABLE edu.attempt_records
    RENAME CONSTRAINT attempt_question_results_pkey TO attempt_records_pkey;
ALTER TABLE edu.attempt_records
    RENAME CONSTRAINT uq_attempt_question_result_order TO uq_attempt_record_order;
ALTER TABLE edu.attempt_records
    RENAME CONSTRAINT ck_attempt_question_result_mapping TO ck_attempt_record_mapping;

ALTER INDEX IF EXISTS edu.idx_attempt_question_results_attempt_order
    RENAME TO idx_attempt_records_attempt_order;

COMMIT;
