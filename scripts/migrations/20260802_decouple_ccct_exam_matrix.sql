BEGIN;

-- Preserve the content that CCCT originally supplied before removing the link.
UPDATE edu.exam_matrix_rows matrix_row
SET topic = item.content
FROM edu.ccct_items item
WHERE matrix_row.ccct_item_id = item.id
  AND (matrix_row.topic IS NULL OR btrim(matrix_row.topic) = '');

DROP INDEX IF EXISTS edu.uq_exam_matrix_ccct_item;
DROP INDEX IF EXISTS edu.idx_exam_matrix_rows_ccct_item;
DROP INDEX IF EXISTS edu.idx_exam_matrices_ccct_profile;

ALTER TABLE edu.exam_matrix_rows
    DROP CONSTRAINT IF EXISTS exam_matrix_rows_ccct_item_id_fkey,
    DROP COLUMN IF EXISTS ccct_item_id;

ALTER TABLE edu.exam_matrices
    DROP CONSTRAINT IF EXISTS exam_matrices_ccct_profile_id_fkey,
    DROP COLUMN IF EXISTS ccct_profile_id;

COMMIT;
