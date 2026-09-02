CREATE TABLE IF NOT EXISTS edu.attempt_question_responses
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES edu.attempts (id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES edu.questions (id) ON DELETE RESTRICT,
    question_type VARCHAR(30) NOT NULL CHECK (question_type = 'MULTIPLE_CHOICE'),
    selected_answer_number BIGINT,
    response_status VARCHAR(20) NOT NULL CHECK (response_status IN ('ANSWERED', 'SKIPPED')),
    is_correct_snapshot BOOLEAN,
    score_ratio DOUBLE PRECISION CHECK (score_ratio IS NULL OR (score_ratio >= 0 AND score_ratio <= 1)),
    grading_revision INTEGER NOT NULL DEFAULT 1 CHECK (grading_revision > 0),
    finalized_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attempt_question_response UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_attempt_question_selected_answer
        FOREIGN KEY (selected_answer_number, question_id)
        REFERENCES edu.answers (answer_number, question_id),
    CONSTRAINT ck_attempt_question_response_state CHECK (
        (response_status = 'ANSWERED' AND selected_answer_number IS NOT NULL
            AND is_correct_snapshot IS NOT NULL AND score_ratio IS NOT NULL)
        OR
        (response_status = 'SKIPPED' AND selected_answer_number IS NULL
            AND is_correct_snapshot IS NULL AND score_ratio IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_attempt_question_response_attempt
    ON edu.attempt_question_responses (attempt_id);
CREATE INDEX IF NOT EXISTS idx_attempt_question_response_question
    ON edu.attempt_question_responses (question_id);
