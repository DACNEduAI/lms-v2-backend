BEGIN;

ALTER TABLE edu.quiz3_question_provenance
    ADD COLUMN IF NOT EXISTS qbank2_selection_mode VARCHAR(30),
    ADD COLUMN IF NOT EXISTS qbank2_content_group_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS qbank2_content_group_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS qbank2_example_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS qbank2_reference_snapshot JSONB;
ALTER TABLE edu.quiz3_question_provenance
    DROP CONSTRAINT IF EXISTS quiz3_question_provenance_qbank2_selection_mode_check;
ALTER TABLE edu.quiz3_question_provenance
    ADD CONSTRAINT quiz3_question_provenance_qbank2_selection_mode_check
        CHECK(qbank2_selection_mode IS NULL OR qbank2_selection_mode IN('AUTO_SELECTED','RAG_FALLBACK','MANUAL_OVERRIDE'));

CREATE TABLE IF NOT EXISTS edu.quiz3_qbank2_regeneration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES edu.quiz3_assessment_variants(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE, requested_by UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    example_ids JSONB NOT NULL DEFAULT '[]'::jsonb, request_payload JSONB NOT NULL, base_question_version BIGINT NOT NULL CHECK(base_question_version >= 0),
    status VARCHAR(40) NOT NULL CHECK(status IN('QUEUED','RUNNING','APPLIED_TO_WORKING_DRAFT','FAILED','CANCELLED','TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED', error_code VARCHAR(80), error_message TEXT, lease_until TIMESTAMPTZ,
    started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz3_qbank2_regeneration_active_job ON edu.quiz3_qbank2_regeneration_jobs(question_id) WHERE status IN('QUEUED','RUNNING');

CREATE TABLE IF NOT EXISTS edu.quiz3_qbank2_regeneration_drafts (
    question_id UUID PRIMARY KEY REFERENCES edu.questions(id) ON DELETE CASCADE, assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES edu.quiz3_assessment_variants(id) ON DELETE CASCADE, job_id UUID NOT NULL REFERENCES edu.quiz3_qbank2_regeneration_jobs(id) ON DELETE CASCADE,
    base_question_version BIGINT NOT NULL CHECK(base_question_version >= 0), content TEXT NOT NULL, content_format VARCHAR(30) NOT NULL,
    explanation TEXT, explanation_format VARCHAR(30) NOT NULL, answers JSONB NOT NULL, content_group_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    content_group_codes JSONB NOT NULL DEFAULT '[]'::jsonb, example_ids JSONB NOT NULL DEFAULT '[]'::jsonb, reference_snapshot JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE edu.quiz3_qbank2_regeneration_drafts
    ADD COLUMN IF NOT EXISTS reference_snapshot JSONB;

COMMIT;
