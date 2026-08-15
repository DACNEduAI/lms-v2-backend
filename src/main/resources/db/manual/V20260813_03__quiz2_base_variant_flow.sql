-- Quiz2 workflow v2: generate and review Quiz 001 directly, then clone variants.
-- Question Bank tables and data intentionally remain untouched for future reuse.

ALTER TABLE edu.assessments
    ALTER COLUMN variant_count DROP NOT NULL,
    ALTER COLUMN variant_count DROP DEFAULT,
    ALTER COLUMN option_count DROP NOT NULL,
    ALTER COLUMN option_count DROP DEFAULT,
    ADD COLUMN IF NOT EXISTS base_generation_job_id UUID
        REFERENCES edu.quiz_generation_jobs (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS base_confirmed_by UUID
        REFERENCES edu.users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS base_confirmed_at TIMESTAMP;

UPDATE edu.assessments SET current_step = 9 WHERE current_step > 9;
ALTER TABLE edu.assessments DROP CONSTRAINT IF EXISTS assessments_current_step_check;
ALTER TABLE edu.assessments ADD CONSTRAINT assessments_current_step_check
    CHECK (current_step BETWEEN 1 AND 9);

ALTER TABLE edu.assessment_variants
    ADD COLUMN IF NOT EXISTS variant_kind VARCHAR(20) NOT NULL DEFAULT 'SHUFFLED';
ALTER TABLE edu.assessment_variants DROP CONSTRAINT IF EXISTS assessment_variants_variant_kind_check;
ALTER TABLE edu.assessment_variants ADD CONSTRAINT assessment_variants_variant_kind_check
    CHECK (variant_kind IN ('BASE', 'SHUFFLED'));

UPDATE edu.assessment_variants variant
SET variant_kind = CASE WHEN variant.order_index = 0 THEN 'BASE' ELSE 'SHUFFLED' END;

CREATE UNIQUE INDEX IF NOT EXISTS uq_assessment_base_variant
    ON edu.assessment_variants (assessment_id) WHERE variant_kind = 'BASE';
