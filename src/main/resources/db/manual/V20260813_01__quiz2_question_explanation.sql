BEGIN;

ALTER TABLE edu.question_drafts
    ADD COLUMN IF NOT EXISTS explanation TEXT,
    ADD COLUMN IF NOT EXISTS explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';

ALTER TABLE edu.question_bank_questions
    ADD COLUMN IF NOT EXISTS explanation TEXT,
    ADD COLUMN IF NOT EXISTS explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';

ALTER TABLE edu.questions
    ADD COLUMN IF NOT EXISTS explanation TEXT,
    ADD COLUMN IF NOT EXISTS explanation_format VARCHAR(30) NOT NULL DEFAULT 'PLAIN_TEXT';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_question_drafts_explanation_format'
          AND conrelid = 'edu.question_drafts'::regclass
    ) THEN
        ALTER TABLE edu.question_drafts ADD CONSTRAINT ck_question_drafts_explanation_format
            CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_question_bank_questions_explanation_format'
          AND conrelid = 'edu.question_bank_questions'::regclass
    ) THEN
        ALTER TABLE edu.question_bank_questions ADD CONSTRAINT ck_question_bank_questions_explanation_format
            CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_questions_explanation_format'
          AND conrelid = 'edu.questions'::regclass
    ) THEN
        ALTER TABLE edu.questions ADD CONSTRAINT ck_questions_explanation_format
            CHECK (explanation_format IN ('PLAIN_TEXT', 'TIPTAP_HTML'));
    END IF;
END $$;

UPDATE edu.question_drafts draft
SET explanation = correct.explanation,
    explanation_format = COALESCE(correct.explanation_format, 'PLAIN_TEXT')
FROM (
    SELECT DISTINCT ON (question_draft_id)
           question_draft_id, explanation, explanation_format
    FROM edu.question_draft_answers
    WHERE is_correct
    ORDER BY question_draft_id, order_index, id
) correct
WHERE draft.id = correct.question_draft_id
  AND draft.assessment_id IS NOT NULL
  AND draft.explanation IS NULL
  AND NULLIF(BTRIM(correct.explanation), '') IS NOT NULL;

UPDATE edu.question_bank_questions bank
SET explanation = correct.explanation,
    explanation_format = COALESCE(correct.explanation_format, 'PLAIN_TEXT')
FROM (
    SELECT DISTINCT ON (question_bank_question_id)
           question_bank_question_id, explanation, explanation_format
    FROM edu.question_bank_answers
    WHERE is_correct
    ORDER BY question_bank_question_id, order_index, id
) correct
WHERE bank.id = correct.question_bank_question_id
  AND bank.assessment_id IS NOT NULL
  AND bank.explanation IS NULL
  AND NULLIF(BTRIM(correct.explanation), '') IS NOT NULL;

WITH correct AS (
    SELECT DISTINCT ON (question.id)
           question.id AS question_id, answer.explanation, answer.explanation_format
    FROM edu.questions question
    JOIN edu.assessment_variants variant ON variant.quiz_id = question.quiz_id
    JOIN edu.answers answer ON answer.question_id = question.id AND answer.is_correct
    ORDER BY question.id, answer.answer_number
)
UPDATE edu.questions question
SET explanation = correct.explanation,
    explanation_format = COALESCE(correct.explanation_format, 'PLAIN_TEXT')
FROM correct
WHERE question.id = correct.question_id
  AND question.explanation IS NULL
  AND NULLIF(BTRIM(correct.explanation), '') IS NOT NULL;

UPDATE edu.question_drafts
SET explanation = NULL
WHERE explanation IS NOT NULL
  AND NULLIF(BTRIM(explanation), '') IS NULL;

UPDATE edu.question_bank_questions
SET explanation = NULL
WHERE explanation IS NOT NULL
  AND NULLIF(BTRIM(explanation), '') IS NULL;

UPDATE edu.questions
SET explanation = NULL
WHERE explanation IS NOT NULL
  AND NULLIF(BTRIM(explanation), '') IS NULL;

COMMIT;
