SET search_path TO edu;

CREATE TABLE IF NOT EXISTS quiz_releases
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id        UUID      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    release_number INTEGER   NOT NULL CHECK (release_number > 0),
    quiz_version   BIGINT    NOT NULL CHECK (quiz_version >= 0),
    active         BOOLEAN   NOT NULL DEFAULT true,
    published_by   UUID REFERENCES users (id) ON DELETE SET NULL,
    published_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quiz_release_number UNIQUE (quiz_id, release_number),
    CONSTRAINT uq_quiz_release_identity UNIQUE (id, quiz_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_quiz_release_active
    ON quiz_releases (quiz_id) WHERE active = true;

CREATE TABLE IF NOT EXISTS quiz_release_code_runner_bindings
(
    quiz_release_id       UUID NOT NULL REFERENCES quiz_releases (id) ON DELETE CASCADE,
    question_id           UUID NOT NULL REFERENCES questions (id) ON DELETE RESTRICT,
    code_runner_config_id UUID NOT NULL REFERENCES code_runner_configs (id) ON DELETE RESTRICT,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (quiz_release_id, question_id),
    CONSTRAINT uq_quiz_release_config UNIQUE (quiz_release_id, code_runner_config_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_release_code_runner_question
    ON quiz_release_code_runner_bindings (question_id, quiz_release_id);

-- Preserve existing published quizzes during rollout. A Code Runner question
-- is bound only when it already has a verified revision.
INSERT INTO quiz_releases
    (id, quiz_id, release_number, quiz_version, active, published_by, published_at)
SELECT gen_random_uuid(), q.id, 1, q.version, true, q.published_by,
       COALESCE(q.published_at, CURRENT_TIMESTAMP)
FROM quizzes q
WHERE q.lifecycle_status = 'PUBLISHED'
  AND NOT EXISTS (SELECT 1 FROM quiz_releases r WHERE r.quiz_id = q.id);

INSERT INTO quiz_release_code_runner_bindings
    (quiz_release_id, question_id, code_runner_config_id)
SELECT r.id, q.id, config.id
FROM quiz_releases r
JOIN questions q ON q.quiz_id = r.quiz_id AND q.type = 'CODE_RUNNER'
JOIN LATERAL (
    SELECT c.id
    FROM code_runner_configs c
    WHERE c.question_id = q.id
      AND c.contract_status = 'VERIFIED_EXECUTION_GOLD'
    ORDER BY c.revision DESC
    LIMIT 1
) config ON true
ON CONFLICT (quiz_release_id, question_id) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM quiz_releases r
        JOIN questions q ON q.quiz_id = r.quiz_id AND q.type = 'CODE_RUNNER'
        LEFT JOIN quiz_release_code_runner_bindings b
          ON b.quiz_release_id = r.id AND b.question_id = q.id
        WHERE r.active = true AND b.question_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot migrate published Quiz Release: a Code Runner question has no verified revision';
    END IF;
END $$;

ALTER TABLE attempts ADD COLUMN IF NOT EXISTS quiz_release_id UUID;

UPDATE attempts a
SET quiz_release_id = r.id
FROM quiz_releases r
WHERE r.quiz_id = a.quiz_id AND r.active = true AND a.quiz_release_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM attempts WHERE quiz_release_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot require attempts.quiz_release_id: some attempts have no published Quiz Release';
    END IF;
END $$;

ALTER TABLE attempts
    ALTER COLUMN quiz_release_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_attempt_quiz_release') THEN
        ALTER TABLE attempts ADD CONSTRAINT fk_attempt_quiz_release
            FOREIGN KEY (quiz_release_id, quiz_id)
            REFERENCES quiz_releases (id, quiz_id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_attempts_release ON attempts (quiz_release_id);
