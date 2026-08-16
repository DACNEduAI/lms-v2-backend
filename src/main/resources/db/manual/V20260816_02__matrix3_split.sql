-- Split the experimental Matrix2 RELATION mode into the independent Matrix3 model.
-- Historical Quiz3 jobs keep their request_snapshot; the experimental relation
-- matrix records and their Assessment source links are deliberately removed.
BEGIN;

-- These temporary columns make the cleanup safe both for the currently deployed
-- experiment and for a database that skipped the abandoned V20260816_01 file.
ALTER TABLE edu.exam_matrices_2 ADD COLUMN IF NOT EXISTS configuration_mode VARCHAR(20) NOT NULL DEFAULT 'QUOTA';
ALTER TABLE edu.exam_matrix_2_cell_counts ADD COLUMN IF NOT EXISTS is_related BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS edu.exam_matrices_3 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL CHECK (btrim(title)<>''), description TEXT,
    total_question_count INTEGER NOT NULL CHECK(total_question_count>0),
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK(lifecycle_status IN('DRAFT','PUBLISHED')),
    version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0), published_by UUID REFERENCES edu.users(id) ON DELETE SET NULL,
    published_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS edu.exam_matrix_3_custom_learning_levels (
    id UUID PRIMARY KEY, matrix_id UUID NOT NULL REFERENCES edu.exam_matrices_3(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL CHECK(btrim(name)<>''), sort_order INTEGER NOT NULL CHECK(sort_order>=0)
);
CREATE TABLE IF NOT EXISTS edu.exam_matrix_3_rows (
    id UUID PRIMARY KEY, matrix_id UUID NOT NULL REFERENCES edu.exam_matrices_3(id) ON DELETE CASCADE,
    chapter_id UUID REFERENCES edu.chapters(chapter_id) ON DELETE SET NULL,
    mapped_learning_outcome_id UUID REFERENCES edu.learning_outcomes(id) ON DELETE SET NULL,
    sort_order INTEGER NOT NULL CHECK(sort_order>=0)
);
CREATE TABLE IF NOT EXISTS edu.exam_matrix_3_cell_relations (
    row_id UUID NOT NULL REFERENCES edu.exam_matrix_3_rows(id) ON DELETE CASCADE,
    custom_learning_level_id UUID NOT NULL REFERENCES edu.exam_matrix_3_custom_learning_levels(id) ON DELETE CASCADE,
    PRIMARY KEY(row_id,custom_learning_level_id)
);
CREATE INDEX IF NOT EXISTS exam_matrices_3_course_idx ON edu.exam_matrices_3(course_id);
CREATE INDEX IF NOT EXISTS exam_matrix_3_rows_matrix_idx ON edu.exam_matrix_3_rows(matrix_id,sort_order);

ALTER TABLE edu.quiz3_assessments ADD COLUMN IF NOT EXISTS matrix_source_type VARCHAR(20);
ALTER TABLE edu.quiz3_assessments ADD COLUMN IF NOT EXISTS matrix3_id UUID REFERENCES edu.exam_matrices_3(id) ON DELETE RESTRICT;
ALTER TABLE edu.quiz3_assessments ADD COLUMN IF NOT EXISTS matrix3_version BIGINT;
DO $$ BEGIN ALTER TABLE edu.quiz3_assessments ADD CONSTRAINT quiz3_assessments_matrix_source_type_check
    CHECK(matrix_source_type IS NULL OR matrix_source_type IN('MATRIX2','MATRIX3')); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS matrix_source_type VARCHAR(20);
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS matrix3_id UUID REFERENCES edu.exam_matrices_3(id) ON DELETE RESTRICT;
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS matrix3_version BIGINT;
DO $$ BEGIN ALTER TABLE edu.quiz3_generation_jobs ADD CONSTRAINT quiz3_generation_jobs_matrix_source_type_check
    CHECK(matrix_source_type IN('MATRIX2','MATRIX3','LEGACY')); EXCEPTION WHEN duplicate_object THEN NULL; END $$;
ALTER TABLE edu.quiz3_generation_jobs ALTER COLUMN matrix2_id DROP NOT NULL;
ALTER TABLE edu.quiz3_generation_jobs ALTER COLUMN matrix2_version DROP NOT NULL;

ALTER TABLE edu.quiz3_question_provenance ADD COLUMN IF NOT EXISTS matrix3_row_id UUID REFERENCES edu.exam_matrix_3_rows(id) ON DELETE SET NULL;
ALTER TABLE edu.quiz3_question_provenance ADD COLUMN IF NOT EXISTS matrix3_custom_learning_level_id UUID REFERENCES edu.exam_matrix_3_custom_learning_levels(id) ON DELETE SET NULL;

-- Matrix2 quota sources remain normal Matrix2 sources. Relation sources are history-only:
-- remove their live source references before deleting the experimental configuration.
UPDATE edu.quiz3_assessments SET matrix_source_type='MATRIX2' WHERE matrix2_id IS NOT NULL
  AND matrix2_id NOT IN (SELECT id FROM edu.exam_matrices_2 WHERE configuration_mode='RELATION');
UPDATE edu.quiz3_generation_jobs SET matrix_source_type='MATRIX2' WHERE matrix2_id IS NOT NULL
  AND matrix2_id NOT IN (SELECT id FROM edu.exam_matrices_2 WHERE configuration_mode='RELATION');
UPDATE edu.quiz3_assessments SET matrix2_id=NULL,matrix2_version=NULL,matrix_source_type=NULL
  WHERE matrix2_id IN (SELECT id FROM edu.exam_matrices_2 WHERE configuration_mode='RELATION');
UPDATE edu.quiz3_generation_jobs SET matrix2_id=NULL,matrix2_version=NULL,matrix_source_type='LEGACY'
  WHERE matrix2_id IN (SELECT id FROM edu.exam_matrices_2 WHERE configuration_mode='RELATION');
DELETE FROM edu.exam_matrices_2 WHERE configuration_mode='RELATION';

ALTER TABLE edu.quiz3_assessments DROP CONSTRAINT IF EXISTS quiz3_assessments_relation_question_count_check;
ALTER TABLE edu.quiz3_assessments DROP COLUMN IF EXISTS relation_question_count;
ALTER TABLE edu.exam_matrices_2 DROP CONSTRAINT IF EXISTS exam_matrices_2_configuration_mode_check;
ALTER TABLE edu.exam_matrices_2 DROP COLUMN IF EXISTS configuration_mode;
ALTER TABLE edu.exam_matrix_2_cell_counts DROP COLUMN IF EXISTS is_related;

COMMIT;
