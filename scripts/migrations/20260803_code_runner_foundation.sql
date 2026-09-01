BEGIN;

CREATE TABLE IF NOT EXISTS edu.code_runner_configs
(
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id                UUID         NOT NULL REFERENCES edu.questions (id) ON DELETE CASCADE,
    schema_version             VARCHAR(20)  NOT NULL CHECK (schema_version = '2.0.0'),
    contract_id                VARCHAR(150) NOT NULL,
    contract_version           VARCHAR(20)  NOT NULL,
    contract_status            VARCHAR(50)  NOT NULL,
    title                      VARCHAR(500) NOT NULL CHECK (btrim(title) <> ''),
    statement                  TEXT         NOT NULL CHECK (btrim(statement) <> ''),
    execution_profile_id       VARCHAR(100) NOT NULL,
    execution_profile_version  VARCHAR(20)  NOT NULL,
    archetype_id               VARCHAR(100) NOT NULL,
    archetype_version          VARCHAR(20)  NOT NULL,
    comparator_id              VARCHAR(100) NOT NULL,
    comparator_version         VARCHAR(20)  NOT NULL,
    entry_point                VARCHAR(150) NOT NULL,
    signature_json             JSONB        NOT NULL,
    starter_code               TEXT         NOT NULL CHECK (btrim(starter_code) <> ''),
    banned_api_patterns_json   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    reference_solution         TEXT         NOT NULL CHECK (btrim(reference_solution) <> ''),
    revision                   INTEGER      NOT NULL CHECK (revision > 0),
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_runner_config_revision UNIQUE (question_id, revision)
);

CREATE INDEX IF NOT EXISTS idx_code_runner_configs_question_latest
    ON edu.code_runner_configs (question_id, revision DESC);

CREATE TABLE IF NOT EXISTS edu.code_runner_test_cases
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_runner_config_id UUID         NOT NULL REFERENCES edu.code_runner_configs (id) ON DELETE CASCADE,
    external_test_id      VARCHAR(150) NOT NULL,
    category              VARCHAR(150) NOT NULL,
    visibility            VARCHAR(10)  NOT NULL CHECK (visibility IN ('PUBLIC', 'HIDDEN')),
    arguments_json        JSONB        NOT NULL,
    expected_output_json  JSONB        NOT NULL,
    points                DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (points >= 0),
    ordinal               INTEGER      NOT NULL CHECK (ordinal >= 0),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_runner_test_external UNIQUE (code_runner_config_id, external_test_id),
    CONSTRAINT uq_code_runner_test_ordinal UNIQUE (code_runner_config_id, ordinal),
    CONSTRAINT ck_code_runner_test_points CHECK (
        (visibility = 'PUBLIC' AND points = 0) OR
        (visibility = 'HIDDEN' AND points > 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_code_runner_tests_config_visibility
    ON edu.code_runner_test_cases (code_runner_config_id, visibility, ordinal);

CREATE TABLE IF NOT EXISTS edu.code_submissions
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id           UUID         NOT NULL REFERENCES edu.questions (id) ON DELETE RESTRICT,
    code_runner_config_id UUID         NOT NULL REFERENCES edu.code_runner_configs (id) ON DELETE RESTRICT,
    attempt_id            UUID         NOT NULL REFERENCES edu.attempts (id) ON DELETE CASCADE,
    student_id            UUID         NOT NULL REFERENCES edu.students (user_id) ON DELETE RESTRICT,
    source_code           TEXT         NOT NULL CHECK (btrim(source_code) <> ''),
    idempotency_key       VARCHAR(255) NOT NULL,
    submission_mode       VARCHAR(10)  NOT NULL CHECK (submission_mode IN ('RUN', 'SUBMIT')),
    status                VARCHAR(40)  NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    verdict               VARCHAR(40),
    score                 DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (score >= 0),
    max_score             DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (max_score >= 0),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP,
    CONSTRAINT uq_code_submission_idempotency UNIQUE (student_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_code_submissions_attempt_question
    ON edu.code_submissions (attempt_id, question_id, submission_mode, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_code_submissions_processing
    ON edu.code_submissions (status, created_at) WHERE status IN ('PENDING', 'PROCESSING');

CREATE TABLE IF NOT EXISTS edu.code_submission_test_results
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id        UUID        NOT NULL REFERENCES edu.code_submissions (id) ON DELETE CASCADE,
    test_case_id         UUID        NOT NULL REFERENCES edu.code_runner_test_cases (id) ON DELETE RESTRICT,
    execution_status     VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (execution_status IN ('PENDING', 'PROCESSING', 'COMPLETED')),
    verdict              VARCHAR(40),
    points_earned        DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (points_earned >= 0),
    time_ms              DOUBLE PRECISION,
    memory_kb            BIGINT,
    diagnostic           TEXT,
    engine_submission_id VARCHAR(255),
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_submission_test UNIQUE (submission_id, test_case_id)
);

COMMIT;
