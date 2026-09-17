BEGIN;

CREATE TABLE IF NOT EXISTS edu.learner_states
(
    id            UUID PRIMARY KEY,
    student_id    UUID      NOT NULL,
    course_id     UUID      NOT NULL,
    attempt_id    UUID      NOT NULL REFERENCES edu.attempts (id) ON DELETE CASCADE,
    calculated_at TIMESTAMP NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_learner_states_attempt UNIQUE (attempt_id)
);

CREATE INDEX IF NOT EXISTS idx_learner_states_student_course_calculated
    ON edu.learner_states (student_id, course_id, calculated_at, id);

CREATE TABLE IF NOT EXISTS edu.learner_state_outcomes
(
    learner_state_id    UUID        NOT NULL REFERENCES edu.learner_states (id) ON DELETE CASCADE,
    learning_outcome_id UUID        NOT NULL,
    probability         DOUBLE PRECISION,
    prediction_status   VARCHAR(50) NOT NULL,
    PRIMARY KEY (learner_state_id, learning_outcome_id),
    CONSTRAINT ck_learner_state_outcome_value CHECK (
        (prediction_status = 'PREDICTED' AND probability IS NOT NULL
            AND probability >= 0.0 AND probability <= 1.0)
        OR
        (prediction_status = 'INSUFFICIENT_PREDICTION' AND probability IS NULL)
    )
);

COMMIT;
