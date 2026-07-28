BEGIN;

SET search_path TO edu;

ALTER TABLE exam_matrix_row_learning_outcomes
    ADD COLUMN is_selected BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE custom_learning_level_learning_outcomes
    ADD COLUMN is_selected BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE custom_learning_levels
    ADD COLUMN matrix_id UUID;

ALTER TABLE exam_matrix_row_learning_level_counts
    ADD COLUMN matrix_id UUID;

ALTER TABLE custom_learning_levels
    DROP CONSTRAINT custom_learning_levels_course_name_key;
DROP INDEX custom_learning_levels_course_code_key;

CREATE TEMP TABLE level_matrix_clone
(
    old_level_id UUID NOT NULL,
    matrix_id    UUID NOT NULL,
    new_level_id UUID NOT NULL,
    PRIMARY KEY (old_level_id, matrix_id)
) ON COMMIT DROP;

INSERT INTO level_matrix_clone (old_level_id, matrix_id, new_level_id)
SELECT level.id, matrix.id, gen_random_uuid()
FROM custom_learning_levels level
JOIN exam_matrices matrix ON matrix.course_id = level.course_id;

INSERT INTO custom_learning_levels
    (id, course_id, matrix_id, code, name, description, sort_order, created_at, updated_at)
SELECT clone.new_level_id, original.course_id, clone.matrix_id,
       original.code, original.name, original.description, original.sort_order,
       original.created_at, original.updated_at
FROM level_matrix_clone clone
JOIN custom_learning_levels original ON original.id = clone.old_level_id;

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
SELECT clone.new_level_id, mapping.learning_outcome_id, mapping.is_selected
FROM level_matrix_clone clone
JOIN custom_learning_level_learning_outcomes mapping
  ON mapping.custom_learning_level_id = clone.old_level_id;

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
SELECT clone.new_level_id, outcome.id, FALSE
FROM level_matrix_clone clone
JOIN exam_matrices matrix ON matrix.id = clone.matrix_id
JOIN learning_outcomes outcome ON outcome.course_id = matrix.course_id
ON CONFLICT (custom_learning_level_id, learning_outcome_id) DO NOTHING;

UPDATE exam_matrix_row_learning_level_counts count_record
SET custom_learning_level_id = clone.new_level_id,
    matrix_id = matrix_row.matrix_id
FROM exam_matrix_rows matrix_row
JOIN level_matrix_clone clone ON clone.matrix_id = matrix_row.matrix_id
WHERE matrix_row.id = count_record.exam_matrix_row_id
  AND clone.old_level_id = count_record.custom_learning_level_id;

DELETE FROM custom_learning_levels
WHERE matrix_id IS NULL;

ALTER TABLE custom_learning_levels
    DROP CONSTRAINT custom_learning_levels_course_id_fkey,
    DROP COLUMN course_id,
    ALTER COLUMN matrix_id SET NOT NULL,
    ADD CONSTRAINT custom_learning_levels_matrix_id_fkey
        FOREIGN KEY (matrix_id) REFERENCES exam_matrices (id) ON DELETE CASCADE,
    ADD CONSTRAINT custom_learning_levels_matrix_name_key UNIQUE (matrix_id, name),
    ADD CONSTRAINT custom_learning_levels_id_matrix_key UNIQUE (id, matrix_id);

CREATE UNIQUE INDEX custom_learning_levels_matrix_code_key
    ON custom_learning_levels (matrix_id, lower(btrim(code)));

INSERT INTO exam_matrix_row_learning_outcomes
    (exam_matrix_row_id, learning_outcome_id, is_selected)
SELECT matrix_row.id, chapter_outcome.learning_outcome_id, FALSE
FROM exam_matrix_rows matrix_row
JOIN chapter_learning_outcomes chapter_outcome
  ON chapter_outcome.chapter_id = matrix_row.chapter_id
ON CONFLICT (exam_matrix_row_id, learning_outcome_id) DO NOTHING;

ALTER TABLE exam_matrix_rows
    ADD CONSTRAINT exam_matrix_rows_id_matrix_key UNIQUE (id, matrix_id);

UPDATE exam_matrix_row_learning_level_counts count_record
SET matrix_id = matrix_row.matrix_id
FROM exam_matrix_rows matrix_row
WHERE matrix_row.id = count_record.exam_matrix_row_id
  AND count_record.matrix_id IS NULL;

ALTER TABLE exam_matrix_row_learning_level_counts
    DROP CONSTRAINT exam_matrix_row_learning_level_counts_row_id_fkey,
    DROP CONSTRAINT exam_matrix_row_learning_level_counts_level_id_fkey,
    ALTER COLUMN matrix_id SET NOT NULL,
    ADD CONSTRAINT exam_matrix_row_learning_level_counts_row_id_fkey
        FOREIGN KEY (exam_matrix_row_id, matrix_id)
            REFERENCES exam_matrix_rows (id, matrix_id) ON DELETE CASCADE,
    ADD CONSTRAINT exam_matrix_row_learning_level_counts_level_id_fkey
        FOREIGN KEY (custom_learning_level_id, matrix_id)
            REFERENCES custom_learning_levels (id, matrix_id) ON DELETE CASCADE;

CREATE INDEX exam_matrix_row_learning_outcomes_outcome_idx
    ON exam_matrix_row_learning_outcomes (learning_outcome_id);
CREATE INDEX exam_matrix_row_learning_outcomes_selected_idx
    ON exam_matrix_row_learning_outcomes (exam_matrix_row_id, is_selected);
CREATE INDEX custom_learning_level_outcomes_outcome_idx
    ON custom_learning_level_learning_outcomes (learning_outcome_id);
CREATE INDEX custom_learning_level_outcomes_selected_idx
    ON custom_learning_level_learning_outcomes (custom_learning_level_id, is_selected);

COMMIT;
