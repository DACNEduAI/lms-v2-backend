-- Durable, question-scoped AI editing for Quiz2 Step 9. This migration is
-- additive and leaves every legacy question/image/formula readable.

BEGIN;

CREATE TABLE IF NOT EXISTS edu.question_ai_working_drafts (
    question_id UUID PRIMARY KEY REFERENCES edu.questions(id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES edu.assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES edu.assessment_variants(id) ON DELETE CASCADE,
    base_question_version BIGINT NOT NULL CHECK (base_question_version >= 0),
    content TEXT NOT NULL,
    content_format VARCHAR(30) NOT NULL CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML')),
    applied_change_index INTEGER NOT NULL DEFAULT 0 CHECK (applied_change_index >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS edu.question_ai_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES edu.assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES edu.assessment_variants(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('ORCHESTRATE', 'TEXT', 'LATEX', 'SVG')),
    status VARCHAR(40) NOT NULL CHECK (status IN (
        'QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION', 'APPLIED_TO_WORKING_DRAFT',
        'FAILED', 'CANCELLED', 'TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED' CHECK (phase IN (
        'QUEUED', 'ORCHESTRATOR', 'TEXT', 'LATEX', 'SVG', 'VALIDATOR', 'CRITIC', 'APPLYING', 'DONE')),
    prompt TEXT NOT NULL DEFAULT '',
    request_payload JSONB NOT NULL,
    action_plan JSONB,
    base_question_version BIGINT NOT NULL CHECK (base_question_version >= 0),
    clarification_question TEXT,
    clarification_answer TEXT,
    error_code VARCHAR(80),
    error_message TEXT,
    warning_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 2),
    action_count INTEGER NOT NULL DEFAULT 0 CHECK (action_count BETWEEN 0 AND 12),
    lease_until TIMESTAMP,
    clarification_expires_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_question_ai_active_job
    ON edu.question_ai_jobs(question_id)
    WHERE status IN ('QUEUED', 'RUNNING', 'NEEDS_CLARIFICATION');
CREATE INDEX IF NOT EXISTS idx_question_ai_job_queue
    ON edu.question_ai_jobs(status, created_at);
CREATE INDEX IF NOT EXISTS idx_question_ai_job_timeline
    ON edu.question_ai_jobs(question_id, created_at DESC);

CREATE TABLE IF NOT EXISTS edu.question_ai_job_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES edu.question_ai_jobs(id) ON DELETE CASCADE,
    action_id VARCHAR(64),
    step_order INTEGER NOT NULL CHECK (step_order >= 0 AND step_order <= 12),
    specialist VARCHAR(30) NOT NULL CHECK (specialist IN ('ORCHESTRATOR', 'TEXT', 'LATEX', 'SVG', 'VALIDATOR', 'CRITIC')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'WARNING')),
    attempt INTEGER NOT NULL DEFAULT 1 CHECK (attempt BETWEEN 1 AND 2),
    input_payload JSONB,
    output_payload JSONB,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_question_ai_steps_job ON edu.question_ai_job_steps(job_id, step_order, started_at);

CREATE TABLE IF NOT EXISTS edu.question_ai_artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES edu.assessments(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    artifact_type VARCHAR(10) NOT NULL CHECK (artifact_type IN ('SVG', 'LATEX')),
    current_revision INTEGER NOT NULL DEFAULT 0 CHECK (current_revision >= 0),
    created_by UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_question_ai_artifact_owner ON edu.question_ai_artifacts(question_id, created_at);

CREATE TABLE IF NOT EXISTS edu.question_ai_artifact_revisions (
    artifact_id UUID NOT NULL REFERENCES edu.question_ai_artifacts(id) ON DELETE CASCADE,
    revision INTEGER NOT NULL CHECK (revision > 0),
    job_id UUID REFERENCES edu.question_ai_jobs(id) ON DELETE SET NULL,
    source_code TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    latex_mode VARCHAR(10) CHECK (latex_mode IN ('INLINE', 'BLOCK')),
    visual_kind VARCHAR(30),
    generator VARCHAR(20) CHECK (generator IN ('SPECIALIZED', 'GENERIC')),
    structured_spec JSONB,
    layout_override JSONB,
    prompt TEXT,
    critic_result JSONB,
    fallback_metadata JSONB,
    storage_key VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (artifact_id, revision)
);

CREATE TABLE IF NOT EXISTS edu.question_ai_change_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    job_id UUID REFERENCES edu.question_ai_jobs(id) ON DELETE SET NULL,
    change_index INTEGER NOT NULL CHECK (change_index > 0),
    before_content TEXT NOT NULL,
    before_content_format VARCHAR(30) NOT NULL,
    after_content TEXT NOT NULL,
    after_content_format VARCHAR(30) NOT NULL,
    artifact_revisions JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(question_id, change_index)
);

ALTER TABLE edu.rich_content_assets
    ADD COLUMN IF NOT EXISTS ai_artifact_id UUID REFERENCES edu.question_ai_artifacts(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_ai_artifact
    ON edu.rich_content_assets(ai_artifact_id) WHERE ai_artifact_id IS NOT NULL;

COMMIT;
