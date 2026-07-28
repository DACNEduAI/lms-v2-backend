BEGIN;

SET search_path TO edu;

DROP TRIGGER IF EXISTS chapter_outcome_matrix_rows_insert
    ON chapter_learning_outcomes;
DROP TRIGGER IF EXISTS chapter_outcome_matrix_rows_delete
    ON chapter_learning_outcomes;

DROP FUNCTION IF EXISTS sync_exam_matrix_rows_after_chapter_outcome_insert();
DROP FUNCTION IF EXISTS sync_exam_matrix_rows_after_chapter_outcome_delete();

COMMIT;
