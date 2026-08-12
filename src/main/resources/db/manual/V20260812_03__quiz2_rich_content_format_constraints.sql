-- Existing databases received the columns in V20260812_01. Add the same
-- enum constraints that schema.sql creates for new installations.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_questions_content_format') THEN
        ALTER TABLE edu.questions ADD CONSTRAINT ck_questions_content_format
            CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_answers_content_format') THEN
        ALTER TABLE edu.answers ADD CONSTRAINT ck_answers_content_format
            CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_answers_explanation_format') THEN
        ALTER TABLE edu.answers ADD CONSTRAINT ck_answers_explanation_format
            CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_question_drafts_content_format') THEN
        ALTER TABLE edu.question_drafts ADD CONSTRAINT ck_question_drafts_content_format
            CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_question_draft_answers_content_format') THEN
        ALTER TABLE edu.question_draft_answers ADD CONSTRAINT ck_question_draft_answers_content_format
            CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_question_draft_answers_explanation_format') THEN
        ALTER TABLE edu.question_draft_answers ADD CONSTRAINT ck_question_draft_answers_explanation_format
            CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_question_bank_questions_content_format') THEN
        ALTER TABLE edu.question_bank_questions ADD CONSTRAINT ck_question_bank_questions_content_format
            CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_question_bank_answers_content_format') THEN
        ALTER TABLE edu.question_bank_answers ADD CONSTRAINT ck_question_bank_answers_content_format
            CHECK (content_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_question_bank_answers_explanation_format') THEN
        ALTER TABLE edu.question_bank_answers ADD CONSTRAINT ck_question_bank_answers_explanation_format
            CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
END $$;
