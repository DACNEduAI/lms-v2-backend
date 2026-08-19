-- P4 stabilization: the application uses FINALIZING while selected Code Runner
-- submissions are still being judged. Existing databases need the same state
-- constraint as schema.sql.
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT con.conname
    INTO constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace ns ON ns.oid = rel.relnamespace
    WHERE ns.nspname = 'edu'
      AND rel.relname = 'attempts'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%status%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE edu.attempts DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE edu.attempts
    ADD CONSTRAINT attempts_status_check
    CHECK (status IN ('STARTED', 'FINALIZING', 'FINISHED', 'ERROR'));

CREATE TABLE IF NOT EXISTS edu.attempt_code_submission_snapshots
(
    attempt_id     UUID NOT NULL REFERENCES edu.attempts (id) ON DELETE CASCADE,
    question_id    UUID NOT NULL REFERENCES edu.questions (id) ON DELETE RESTRICT,
    submission_id  UUID NOT NULL REFERENCES edu.code_submissions (id) ON DELETE RESTRICT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attempt_id, question_id),
    CONSTRAINT uq_attempt_code_submission_snapshot_submission UNIQUE (attempt_id, submission_id)
);

CREATE INDEX IF NOT EXISTS idx_attempt_code_submission_snapshots_submission
    ON edu.attempt_code_submission_snapshots (submission_id);
