ALTER TABLE edu.exam_matrix_cells DROP COLUMN IF EXISTS question_type;

ALTER TABLE edu.quiz3_question_provenance
    ADD COLUMN IF NOT EXISTS requested_asset_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS effective_asset_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS asset_fallback_reason VARCHAR(255);

ALTER TABLE edu.quiz3_question_provenance
    ADD CONSTRAINT quiz3_question_provenance_requested_asset_type_check
    CHECK (requested_asset_type IN ('TEXT','IMAGE','CODE_MCQ','CODE_RUNNER'));

ALTER TABLE edu.quiz3_question_provenance
    ADD CONSTRAINT quiz3_question_provenance_effective_asset_type_check
    CHECK (effective_asset_type IN ('TEXT','IMAGE','CODE_MCQ','CODE_RUNNER'));
