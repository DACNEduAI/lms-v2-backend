-- Matrix 2 tables for databases that preserve existing data
-- (DATABASE_INITIALIZATION_OPTION=never).  The tables are independent from
-- the existing Exam Matrix and Quiz2 workflows.
BEGIN;

CREATE TABLE IF NOT EXISTS edu.exam_matrices_2
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID         NOT NULL REFERENCES edu.courses (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS edu.exam_matrix_2_custom_learning_levels
(
    id          UUID PRIMARY KEY,
    matrix_id   UUID         NOT NULL REFERENCES edu.exam_matrices_2 (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    sort_order  INTEGER      NOT NULL CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS edu.exam_matrix_2_rows
(
    id                         UUID PRIMARY KEY,
    matrix_id                  UUID NOT NULL REFERENCES edu.exam_matrices_2 (id) ON DELETE CASCADE,
    chapter_id                 UUID REFERENCES edu.chapters (chapter_id) ON DELETE SET NULL,
    mapped_learning_outcome_id UUID REFERENCES edu.learning_outcomes (id) ON DELETE SET NULL,
    sort_order                 INTEGER NOT NULL CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS edu.exam_matrix_2_cell_counts
(
    row_id                   UUID NOT NULL REFERENCES edu.exam_matrix_2_rows (id) ON DELETE CASCADE,
    custom_learning_level_id UUID NOT NULL REFERENCES edu.exam_matrix_2_custom_learning_levels (id) ON DELETE CASCADE,
    question_count           INTEGER NOT NULL DEFAULT 0 CHECK (question_count >= 0),
    PRIMARY KEY (row_id, custom_learning_level_id)
);

CREATE INDEX IF NOT EXISTS exam_matrices_2_course_idx
    ON edu.exam_matrices_2 (course_id);
CREATE INDEX IF NOT EXISTS exam_matrix_2_rows_matrix_idx
    ON edu.exam_matrix_2_rows (matrix_id, sort_order);

COMMIT;
