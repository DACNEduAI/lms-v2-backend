SET search_path TO edu, public;

CREATE TABLE IF NOT EXISTS assessment_webhook_events
(
    event_id    UUID PRIMARY KEY,
    batch_id    UUID NOT NULL REFERENCES assessment_generation_batches (id) ON DELETE CASCADE,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE question_drafts
    ADD COLUMN IF NOT EXISTS code_runner_contract JSONB;

ALTER TABLE question_bank_questions
    ADD COLUMN IF NOT EXISTS code_runner_contract JSONB;

ALTER TABLE assessment_generation_batch_items
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(30) NOT NULL DEFAULT 'MULTIPLE_CHOICE';

ALTER TABLE assessment_generation_batch_items
    DROP CONSTRAINT IF EXISTS assessment_generation_batch_items_question_type_check;

ALTER TABLE assessment_generation_batch_items
    ADD CONSTRAINT assessment_generation_batch_items_question_type_check
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'CODE_RUNNER'));

ALTER TABLE question_drafts
    DROP CONSTRAINT IF EXISTS question_drafts_code_runner_contract_check;

ALTER TABLE question_drafts
    ADD CONSTRAINT question_drafts_code_runner_contract_check CHECK (
        (question_type = 'CODE_RUNNER' AND code_runner_contract IS NOT NULL)
        OR (question_type <> 'CODE_RUNNER' AND code_runner_contract IS NULL)
    ) NOT VALID;

ALTER TABLE question_bank_questions
    DROP CONSTRAINT IF EXISTS question_bank_questions_code_runner_contract_check;

ALTER TABLE question_bank_questions
    ADD CONSTRAINT question_bank_questions_code_runner_contract_check CHECK (
        (question_type = 'CODE_RUNNER' AND code_runner_contract IS NOT NULL)
        OR (question_type <> 'CODE_RUNNER' AND code_runner_contract IS NULL)
    ) NOT VALID;
