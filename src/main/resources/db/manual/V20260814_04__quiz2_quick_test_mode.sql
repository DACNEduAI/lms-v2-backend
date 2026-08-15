-- Quick Test Quiz 001 is generated from ready course materials without a Matrix.
-- Existing assessments and generation jobs remain STANDARD.

ALTER TABLE edu.assessments
    ADD COLUMN IF NOT EXISTS generation_mode VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE edu.assessments DROP CONSTRAINT IF EXISTS assessments_generation_mode_check;
ALTER TABLE edu.assessments ADD CONSTRAINT assessments_generation_mode_check
    CHECK (generation_mode IN ('STANDARD', 'QUICK_TEST'));

ALTER TABLE edu.quiz_generation_jobs
    ALTER COLUMN exam_matrix_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS generation_mode VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE edu.quiz_generation_jobs DROP CONSTRAINT IF EXISTS quiz_generation_jobs_generation_mode_check;
ALTER TABLE edu.quiz_generation_jobs ADD CONSTRAINT quiz_generation_jobs_generation_mode_check
    CHECK (generation_mode IN ('STANDARD', 'QUICK_TEST'));
ALTER TABLE edu.quiz_generation_jobs DROP CONSTRAINT IF EXISTS quiz_generation_jobs_matrix_for_standard_check;
ALTER TABLE edu.quiz_generation_jobs ADD CONSTRAINT quiz_generation_jobs_matrix_for_standard_check
    CHECK (generation_mode = 'QUICK_TEST' OR exam_matrix_id IS NOT NULL);
