BEGIN;

ALTER TABLE edu.exam_matrix_row_learning_level_counts
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(30);

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'exam_matrix_row_learning_level_counts_question_type_check'
          AND conrelid = 'edu.exam_matrix_row_learning_level_counts'::regclass
    ) THEN
        ALTER TABLE edu.exam_matrix_row_learning_level_counts
            ADD CONSTRAINT exam_matrix_row_learning_level_counts_question_type_check
                CHECK (
                    question_type IS NULL OR
                    question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'ESSAY', 'CODE_RUNNER')
                );
    END IF;
END
$$;

-- Existing rows deliberately remain NULL. Their historical type was never stored,
-- so a teacher must confirm/resave the matrix before it can use the new generation flow.
-- New create/update requests are validated by the application and always persist
-- an explicit MULTIPLE_CHOICE value during the Phase 4 MVP.

COMMIT;
