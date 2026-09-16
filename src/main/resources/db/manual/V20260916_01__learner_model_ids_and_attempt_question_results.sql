BEGIN;

ALTER TABLE edu.questions
    ADD COLUMN IF NOT EXISTS kt_question_id BIGINT GENERATED ALWAYS AS IDENTITY;
ALTER TABLE edu.questions
    ALTER COLUMN kt_question_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_questions_kt_question_id
    ON edu.questions (kt_question_id);

ALTER TABLE edu.learning_outcomes
    ADD COLUMN IF NOT EXISTS kt_learning_outcome_id BIGINT GENERATED ALWAYS AS IDENTITY;
ALTER TABLE edu.learning_outcomes
    ALTER COLUMN kt_learning_outcome_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_learning_outcomes_kt_learning_outcome_id
    ON edu.learning_outcomes (kt_learning_outcome_id);

CREATE TABLE IF NOT EXISTS edu.attempt_question_results
(
    attempt_id                     UUID    NOT NULL REFERENCES edu.attempts (id) ON DELETE CASCADE,
    question_id                    UUID    NOT NULL,
    kt_question_id                 BIGINT  NOT NULL,
    interaction_order              INTEGER NOT NULL CHECK (interaction_order >= 0),
    is_correct                     BOOLEAN,
    learning_outcome_id            UUID,
    kt_learning_outcome_id         BIGINT,
    learning_outcome_mapping_count INTEGER NOT NULL DEFAULT 0
        CHECK (learning_outcome_mapping_count >= 0),
    created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attempt_id, question_id),
    CONSTRAINT uq_attempt_question_result_order UNIQUE (attempt_id, interaction_order),
    CONSTRAINT ck_attempt_question_result_mapping CHECK (
        (learning_outcome_mapping_count = 1
            AND learning_outcome_id IS NOT NULL
            AND kt_learning_outcome_id IS NOT NULL)
        OR
        (learning_outcome_mapping_count <> 1
            AND learning_outcome_id IS NULL
            AND kt_learning_outcome_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_attempt_question_results_attempt_order
    ON edu.attempt_question_results (attempt_id, interaction_order);

COMMIT;
