-- Rich content remains backward compatible: legacy text rows are PLAIN_TEXT.
ALTER TABLE edu.questions ADD COLUMN IF NOT EXISTS content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.answers ADD COLUMN IF NOT EXISTS content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.answers ADD COLUMN IF NOT EXISTS explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.question_drafts ADD COLUMN IF NOT EXISTS content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.question_draft_answers ADD COLUMN IF NOT EXISTS content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.question_draft_answers ADD COLUMN IF NOT EXISTS explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.question_bank_questions ADD COLUMN IF NOT EXISTS content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.question_bank_answers ADD COLUMN IF NOT EXISTS content_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';
ALTER TABLE edu.question_bank_answers ADD COLUMN IF NOT EXISTS explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';

ALTER TABLE edu.question_drafts ADD COLUMN IF NOT EXISTS supersedes_bank_question_id UUID
    REFERENCES edu.question_bank_questions (id) ON DELETE SET NULL;
ALTER TABLE edu.question_bank_questions ADD COLUMN IF NOT EXISTS supersedes_bank_question_id UUID
    REFERENCES edu.question_bank_questions (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS edu.rich_content_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_draft_id UUID REFERENCES edu.question_drafts (id) ON DELETE CASCADE,
    question_bank_question_id UUID REFERENCES edu.question_bank_questions (id) ON DELETE CASCADE,
    question_id UUID REFERENCES edu.questions (id) ON DELETE CASCADE,
    storage_key VARCHAR(1024) NOT NULL,
    media_type VARCHAR(255) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size >= 0 AND byte_size <= 10485760),
    sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_rich_content_asset_exactly_one_owner CHECK (
      ((question_draft_id IS NOT NULL)::int + (question_bank_question_id IS NOT NULL)::int + (question_id IS NOT NULL)::int) = 1
    )
);
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_draft ON edu.rich_content_assets(question_draft_id) WHERE question_draft_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_bank ON edu.rich_content_assets(question_bank_question_id) WHERE question_bank_question_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_rich_content_assets_question ON edu.rich_content_assets(question_id) WHERE question_id IS NOT NULL;
