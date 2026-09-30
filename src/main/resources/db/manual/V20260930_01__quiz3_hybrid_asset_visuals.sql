BEGIN;
ALTER TABLE edu.quiz3_generation_jobs DROP CONSTRAINT IF EXISTS quiz3_generation_jobs_status_check;
ALTER TABLE edu.quiz3_generation_jobs ADD CONSTRAINT quiz3_generation_jobs_status_check CHECK(status IN('PENDING','PROCESSING','COMPLETED','COMPLETED_WITH_WARNINGS','FAILED','TIMED_OUT'));
ALTER TABLE edu.quiz3_generation_jobs DROP CONSTRAINT IF EXISTS quiz3_generation_jobs_phase_check;
ALTER TABLE edu.quiz3_generation_jobs ADD CONSTRAINT quiz3_generation_jobs_phase_check CHECK(phase IN('TEXT_GENERATION','VISUAL_GENERATION','FINALIZING','DONE'));
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS processed_visual_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS total_visual_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS failed_visual_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE edu.quiz3_generation_jobs ADD COLUMN IF NOT EXISTS visual_warnings JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE edu.asset_generation_jobs DROP CONSTRAINT IF EXISTS asset_generation_jobs_generation_mode_check;
ALTER TABLE edu.asset_generation_jobs ADD CONSTRAINT asset_generation_jobs_generation_mode_check CHECK(generation_mode IN ('INITIAL','REGENERATE','UPLOAD_ANALYSIS','QUIZ3_VISUAL'));
ALTER TABLE edu.quiz3_rich_content_assets ADD COLUMN IF NOT EXISTS source_asset_id UUID REFERENCES edu.assets(id) ON DELETE SET NULL;
CREATE TABLE IF NOT EXISTS edu.quiz3_question_visuals (
 question_id UUID PRIMARY KEY REFERENCES edu.questions(id) ON DELETE CASCADE, generation_job_id UUID NOT NULL REFERENCES edu.quiz3_generation_jobs(id) ON DELETE CASCADE,
 placement VARCHAR(16) NOT NULL CHECK(placement IN('QUESTION','ANSWER')), answer_index INTEGER, visual_brief TEXT NOT NULL, alt_text TEXT NOT NULL,
 resolution_action VARCHAR(32) NOT NULL CHECK(resolution_action IN('REUSE_ASSET','GENERATE_FROM_ASSET','GENERATE_NEW')), search_query TEXT NOT NULL,
 candidates JSONB NOT NULL DEFAULT '[]'::jsonb, generation_prompt TEXT, selected_asset_id UUID REFERENCES edu.assets(id) ON DELETE SET NULL,
 generated_asset_id UUID REFERENCES edu.assets(id) ON DELETE SET NULL, asset_generation_job_id UUID UNIQUE REFERENCES edu.asset_generation_jobs(id) ON DELETE SET NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN('PENDING','PROCESSING','READY','FAILED')), attempt_count INTEGER NOT NULL DEFAULT 0, error_message TEXT, selector_warning TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK((placement='QUESTION' AND answer_index IS NULL) OR (placement='ANSWER' AND answer_index>=0))
);
CREATE INDEX IF NOT EXISTS idx_quiz3_question_visuals_pending ON edu.quiz3_question_visuals(status,generation_job_id);
CREATE TABLE IF NOT EXISTS edu.asset_vector_sync_outbox (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), asset_id UUID NOT NULL UNIQUE REFERENCES edu.assets(id) ON DELETE CASCADE,
 attempt_count INTEGER NOT NULL DEFAULT 0,last_error TEXT,next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_asset_vector_sync_outbox_pending ON edu.asset_vector_sync_outbox(next_attempt_at);
COMMIT;
