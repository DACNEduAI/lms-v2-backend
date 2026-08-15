-- Additive Quiz3 + Matrix2 lifecycle migration. Quiz2/Matrix1 data is untouched.
BEGIN;

ALTER TABLE edu.exam_matrices_2
    ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS published_by UUID REFERENCES edu.users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;

DO $$ BEGIN
    ALTER TABLE edu.exam_matrices_2 ADD CONSTRAINT exam_matrices_2_lifecycle_status_check
        CHECK (lifecycle_status IN ('DRAFT', 'PUBLISHED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
    ALTER TABLE edu.exam_matrices_2 ADD CONSTRAINT exam_matrices_2_version_check CHECK (version >= 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS edu.quiz3_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description TEXT,
    unlimited BOOLEAN NOT NULL DEFAULT false,
    time_limit_minutes INTEGER CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    pass_score DOUBLE PRECISION CHECK (pass_score IS NULL OR (pass_score >= 0 AND pass_score <= 100)),
    start_time TIMESTAMP,
    due_time TIMESTAMP,
    attempt_limited INTEGER CHECK (attempt_limited IS NULL OR attempt_limited > 0),
    variant_count INTEGER NOT NULL DEFAULT 1 CHECK (variant_count BETWEEN 1 AND 20),
    lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_status IN ('DRAFT','GENERATING','REVIEW_PENDING','READY_TO_PUBLISH','PUBLISHED','ARCHIVED')),
    current_step INTEGER NOT NULL DEFAULT 1 CHECK (current_step BETWEEN 1 AND 9),
    ccct_profile_id UUID REFERENCES edu.ccct_profiles(id) ON DELETE RESTRICT,
    ccct_profile_version BIGINT,
    matrix2_id UUID REFERENCES edu.exam_matrices_2(id) ON DELETE RESTRICT,
    matrix2_version BIGINT,
    base_generation_job_id UUID,
    base_confirmed_by UUID REFERENCES edu.users(id) ON DELETE SET NULL,
    base_confirmed_at TIMESTAMP,
    created_by UUID REFERENCES edu.users(id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    published_by UUID REFERENCES edu.users(id) ON DELETE SET NULL,
    published_at TIMESTAMP,
    last_unpublished_by UUID REFERENCES edu.users(id) ON DELETE SET NULL,
    last_unpublished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (due_time IS NULL OR start_time IS NULL OR due_time > start_time)
);
CREATE INDEX IF NOT EXISTS idx_quiz3_assessments_course_updated
    ON edu.quiz3_assessments(course_id, updated_at DESC, id);

CREATE TABLE IF NOT EXISTS edu.quiz3_generation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
    matrix2_id UUID NOT NULL REFERENCES edu.exam_matrices_2(id) ON DELETE RESTRICT,
    matrix2_version BIGINT NOT NULL,
    quiz_id UUID REFERENCES edu.quizzes(id) ON DELETE SET NULL,
    requested_by UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    request_snapshot JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'TEXT_GENERATION'
        CHECK (phase IN ('TEXT_GENERATION','FINALIZING','DONE')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    generated_question_count INTEGER NOT NULL DEFAULT 0 CHECK (generated_question_count >= 0),
    total_question_count INTEGER NOT NULL CHECK (total_question_count > 0),
    next_attempt_at TIMESTAMPTZ,
    lease_until TIMESTAMPTZ,
    error_code VARCHAR(100),
    error_message TEXT,
    callback_event_id UUID UNIQUE,
    processing_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE edu.quiz3_assessments DROP CONSTRAINT IF EXISTS quiz3_assessments_base_generation_job_id_fkey;
ALTER TABLE edu.quiz3_assessments ADD CONSTRAINT quiz3_assessments_base_generation_job_id_fkey
    FOREIGN KEY (base_generation_job_id) REFERENCES edu.quiz3_generation_jobs(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_quiz3_generation_queue ON edu.quiz3_generation_jobs(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_quiz3_generation_assessment ON edu.quiz3_generation_jobs(assessment_id, created_at DESC);

CREATE TABLE IF NOT EXISTS edu.quiz3_assessment_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    quiz_id UUID NOT NULL UNIQUE REFERENCES edu.quizzes(id) ON DELETE CASCADE,
    variant_code VARCHAR(20) NOT NULL,
    order_index INTEGER NOT NULL CHECK (order_index >= 0),
    shuffle_seed BIGINT NOT NULL,
    variant_kind VARCHAR(20) NOT NULL CHECK (variant_kind IN ('BASE','SHUFFLED')),
    coverage_status VARCHAR(20) NOT NULL DEFAULT 'COMPLIANT' CHECK (coverage_status IN ('COMPLIANT','DRIFTED')),
    coverage_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(assessment_id, variant_code), UNIQUE(assessment_id, order_index)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz3_base_variant ON edu.quiz3_assessment_variants(assessment_id)
    WHERE variant_kind = 'BASE';

CREATE TABLE IF NOT EXISTS edu.quiz3_question_provenance (
    question_id UUID PRIMARY KEY REFERENCES edu.questions(id) ON DELETE CASCADE,
    generation_job_id UUID NOT NULL REFERENCES edu.quiz3_generation_jobs(id) ON DELETE RESTRICT,
    matrix2_row_id UUID REFERENCES edu.exam_matrix_2_rows(id) ON DELETE SET NULL,
    matrix2_custom_learning_level_id UUID REFERENCES edu.exam_matrix_2_custom_learning_levels(id) ON DELETE SET NULL,
    learning_outcome_id UUID REFERENCES edu.learning_outcomes(id) ON DELETE SET NULL,
    source_question_id UUID REFERENCES edu.questions(id) ON DELETE SET NULL,
    allocation_snapshot JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_quiz3_provenance_job ON edu.quiz3_question_provenance(generation_job_id);
CREATE INDEX IF NOT EXISTS idx_quiz3_provenance_allocation
    ON edu.quiz3_question_provenance(matrix2_row_id, matrix2_custom_learning_level_id, learning_outcome_id);

CREATE TABLE IF NOT EXISTS edu.quiz3_question_ai_working_drafts (
    question_id UUID PRIMARY KEY REFERENCES edu.questions(id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES edu.quiz3_assessment_variants(id) ON DELETE CASCADE,
    base_question_version BIGINT NOT NULL CHECK (base_question_version >= 0),
    content TEXT NOT NULL, content_format VARCHAR(30) NOT NULL,
    applied_change_index INTEGER NOT NULL DEFAULT 0 CHECK (applied_change_index >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS edu.quiz3_question_ai_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES edu.quiz3_assessment_variants(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('ORCHESTRATE','TEXT','LATEX','SVG')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('QUEUED','RUNNING','NEEDS_CLARIFICATION','APPLIED_TO_WORKING_DRAFT','FAILED','CANCELLED','TIMED_OUT')),
    phase VARCHAR(30) NOT NULL DEFAULT 'QUEUED', prompt TEXT NOT NULL DEFAULT '',
    request_payload JSONB NOT NULL, action_plan JSONB, base_question_version BIGINT NOT NULL,
    clarification_question TEXT, clarification_answer TEXT, error_code VARCHAR(80), error_message TEXT,
    warning_message TEXT, attempt_count INTEGER NOT NULL DEFAULT 0, action_count INTEGER NOT NULL DEFAULT 0,
    lease_until TIMESTAMP, clarification_expires_at TIMESTAMP, started_at TIMESTAMP, completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz3_question_ai_active_job ON edu.quiz3_question_ai_jobs(question_id)
    WHERE status IN ('QUEUED','RUNNING','NEEDS_CLARIFICATION');
CREATE TABLE IF NOT EXISTS edu.quiz3_question_ai_job_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), job_id UUID NOT NULL REFERENCES edu.quiz3_question_ai_jobs(id) ON DELETE CASCADE,
    action_id VARCHAR(64), step_order INTEGER NOT NULL, specialist VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL,
    attempt INTEGER NOT NULL DEFAULT 1, input_payload JSONB, output_payload JSONB, error_message TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMP
);
CREATE TABLE IF NOT EXISTS edu.quiz3_question_ai_artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), assessment_id UUID NOT NULL REFERENCES edu.quiz3_assessments(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE, artifact_type VARCHAR(10) NOT NULL,
    current_revision INTEGER NOT NULL DEFAULT 0, created_by UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS edu.quiz3_question_ai_artifact_revisions (
    artifact_id UUID NOT NULL REFERENCES edu.quiz3_question_ai_artifacts(id) ON DELETE CASCADE, revision INTEGER NOT NULL,
    job_id UUID REFERENCES edu.quiz3_question_ai_jobs(id) ON DELETE SET NULL, source_code TEXT NOT NULL, source_hash VARCHAR(64) NOT NULL,
    latex_mode VARCHAR(10), visual_kind VARCHAR(30), generator VARCHAR(20), structured_spec JSONB, layout_override JSONB,
    prompt TEXT, critic_result JSONB, fallback_metadata JSONB, storage_key VARCHAR(1024), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(artifact_id, revision)
);
CREATE TABLE IF NOT EXISTS edu.quiz3_question_ai_change_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    job_id UUID REFERENCES edu.quiz3_question_ai_jobs(id) ON DELETE SET NULL, change_index INTEGER NOT NULL,
    before_content TEXT NOT NULL, before_content_format VARCHAR(30) NOT NULL, after_content TEXT NOT NULL,
    after_content_format VARCHAR(30) NOT NULL, artifact_revisions JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(question_id, change_index)
);

COMMIT;
