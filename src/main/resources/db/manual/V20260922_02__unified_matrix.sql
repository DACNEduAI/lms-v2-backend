-- Breaking development reset: Matrix2/Matrix3 and all Matrix-derived Quiz3 history are intentionally discarded.
BEGIN;
DELETE FROM edu.quiz3_question_provenance;
TRUNCATE TABLE edu.quiz3_assessments CASCADE;
DROP TABLE IF EXISTS edu.exam_matrix_3_cell_relations CASCADE;
DROP TABLE IF EXISTS edu.exam_matrix_3_rows CASCADE;
DROP TABLE IF EXISTS edu.exam_matrix_3_custom_learning_levels CASCADE;
DROP TABLE IF EXISTS edu.exam_matrices_3 CASCADE;
DROP TABLE IF EXISTS edu.exam_matrix_2_cell_counts CASCADE;
DROP TABLE IF EXISTS edu.exam_matrix_2_rows CASCADE;
DROP TABLE IF EXISTS edu.exam_matrix_2_custom_learning_levels CASCADE;
DROP TABLE IF EXISTS edu.exam_matrices_2 CASCADE;
CREATE TABLE edu.exam_matrices (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
 title VARCHAR(255) NOT NULL CHECK(btrim(title)<>''), description TEXT, mode VARCHAR(20) NOT NULL CHECK(mode IN('MANUAL','AUTOMATIC')),
 total_question_count INTEGER CHECK(total_question_count IS NULL OR total_question_count>0), lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK(lifecycle_status IN('DRAFT','PUBLISHED')),
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0), published_by UUID REFERENCES edu.users(id) ON DELETE SET NULL, published_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK((mode='MANUAL' AND total_question_count IS NULL) OR (mode='AUTOMATIC' AND total_question_count IS NOT NULL))
);
CREATE TABLE edu.exam_matrix_custom_learning_levels (id UUID PRIMARY KEY,matrix_id UUID NOT NULL REFERENCES edu.exam_matrices(id) ON DELETE CASCADE,name VARCHAR(255) NOT NULL CHECK(btrim(name)<>''),sort_order INTEGER NOT NULL CHECK(sort_order>=0));
CREATE TABLE edu.exam_matrix_rows (id UUID PRIMARY KEY,matrix_id UUID NOT NULL REFERENCES edu.exam_matrices(id) ON DELETE CASCADE,chapter_id UUID REFERENCES edu.chapters(chapter_id) ON DELETE SET NULL,mapped_learning_outcome_id UUID REFERENCES edu.learning_outcomes(id) ON DELETE SET NULL,sort_order INTEGER NOT NULL CHECK(sort_order>=0));
CREATE TABLE edu.exam_matrix_cells (row_id UUID NOT NULL REFERENCES edu.exam_matrix_rows(id) ON DELETE CASCADE,custom_learning_level_id UUID NOT NULL REFERENCES edu.exam_matrix_custom_learning_levels(id) ON DELETE CASCADE,question_count INTEGER CHECK(question_count>0),question_type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE' CHECK(question_type IN('MULTIPLE_CHOICE','CODE_RUNNER')),PRIMARY KEY(row_id,custom_learning_level_id));
CREATE INDEX exam_matrices_course_idx ON edu.exam_matrices(course_id);
CREATE INDEX exam_matrix_rows_matrix_idx ON edu.exam_matrix_rows(matrix_id,sort_order);
ALTER TABLE edu.quiz3_assessments DROP COLUMN IF EXISTS matrix_source_type, DROP COLUMN IF EXISTS matrix2_id, DROP COLUMN IF EXISTS matrix2_version, DROP COLUMN IF EXISTS matrix3_id, DROP COLUMN IF EXISTS matrix3_version;
ALTER TABLE edu.quiz3_assessments ADD COLUMN IF NOT EXISTS matrix_id UUID REFERENCES edu.exam_matrices(id) ON DELETE RESTRICT, ADD COLUMN IF NOT EXISTS matrix_version BIGINT;
ALTER TABLE edu.quiz3_generation_jobs DROP COLUMN IF EXISTS matrix_source_type, DROP COLUMN IF EXISTS matrix2_id, DROP COLUMN IF EXISTS matrix2_version, DROP COLUMN IF EXISTS matrix3_id, DROP COLUMN IF EXISTS matrix3_version;
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS matrix_id UUID REFERENCES edu.exam_matrices(id) ON DELETE RESTRICT, ADD COLUMN IF NOT EXISTS matrix_version BIGINT;
ALTER TABLE edu.quiz3_question_provenance DROP COLUMN IF EXISTS matrix2_row_id, DROP COLUMN IF EXISTS matrix2_custom_learning_level_id, DROP COLUMN IF EXISTS matrix3_row_id, DROP COLUMN IF EXISTS matrix3_custom_learning_level_id;
ALTER TABLE edu.quiz3_question_provenance ADD COLUMN IF NOT EXISTS matrix_row_id UUID REFERENCES edu.exam_matrix_rows(id) ON DELETE SET NULL, ADD COLUMN IF NOT EXISTS matrix_custom_learning_level_id UUID REFERENCES edu.exam_matrix_custom_learning_levels(id) ON DELETE SET NULL;
COMMIT;
