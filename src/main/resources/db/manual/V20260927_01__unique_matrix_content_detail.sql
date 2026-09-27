BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM edu.exam_matrix_rows
        WHERE ccct_item_id IS NOT NULL
        GROUP BY matrix_id, ccct_item_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce one Content Detail per Matrix: duplicate Matrix rows exist';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_exam_matrix_row_content'
          AND conrelid = 'edu.exam_matrix_rows'::regclass
    ) THEN
        ALTER TABLE edu.exam_matrix_rows
            ADD CONSTRAINT uq_exam_matrix_row_content UNIQUE (matrix_id, ccct_item_id);
    END IF;
END $$;

COMMIT;
