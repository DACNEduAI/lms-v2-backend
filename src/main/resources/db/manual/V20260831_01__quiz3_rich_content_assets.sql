BEGIN;

-- Quiz3 PDF export resolves every question's embedded assets through this
-- table, including questions which currently have no images.
CREATE TABLE IF NOT EXISTS edu.quiz3_rich_content_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES edu.questions(id) ON DELETE CASCADE,
    storage_key VARCHAR(1024) NOT NULL,
    media_type VARCHAR(255) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size >= 0 AND byte_size <= 10485760),
    sha256 CHAR(64) NOT NULL,
    ai_artifact_id UUID REFERENCES edu.quiz3_question_ai_artifacts(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quiz3_rich_content_assets_question
    ON edu.quiz3_rich_content_assets(question_id);
CREATE INDEX IF NOT EXISTS idx_quiz3_rich_content_assets_ai_artifact
    ON edu.quiz3_rich_content_assets(ai_artifact_id) WHERE ai_artifact_id IS NOT NULL;

COMMIT;
