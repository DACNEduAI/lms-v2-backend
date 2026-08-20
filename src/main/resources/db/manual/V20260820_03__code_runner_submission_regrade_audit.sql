BEGIN;

CREATE TABLE IF NOT EXISTS edu.code_runner_submission_actions
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id   UUID NOT NULL REFERENCES edu.code_submissions(id) ON DELETE CASCADE,
    attempt_id      UUID NOT NULL REFERENCES edu.attempts(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES edu.questions(id) ON DELETE RESTRICT,
    actor_id        UUID NOT NULL REFERENCES edu.users(id) ON DELETE RESTRICT,
    action          VARCHAR(30) NOT NULL CHECK(action IN('REGRADE')),
    previous_status VARCHAR(30) NOT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_code_runner_submission_actions_submission
    ON edu.code_runner_submission_actions(submission_id,created_at DESC);

COMMIT;
