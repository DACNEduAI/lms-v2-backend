BEGIN;

ALTER TABLE edu.exam_matrix_2_cell_counts
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE';

DO $$ BEGIN
    ALTER TABLE edu.exam_matrix_2_cell_counts
        ADD CONSTRAINT exam_matrix_2_cell_counts_question_type_check
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'CODE_RUNNER'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE edu.exam_matrix_3_cell_relations
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE';

DO $$ BEGIN
    ALTER TABLE edu.exam_matrix_3_cell_relations
        ADD CONSTRAINT exam_matrix_3_cell_relations_question_type_check
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'CODE_RUNNER'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

COMMIT;
