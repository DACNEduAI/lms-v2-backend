SET search_path TO edu, public;

CREATE TABLE IF NOT EXISTS code_runner_verifications (
    id UUID PRIMARY KEY,
    revision_id UUID NOT NULL REFERENCES code_runner_configs(id),
    verified_by UUID NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_profile JSONB NOT NULL,
    judge0_submission_id VARCHAR(100),
    verdict VARCHAR(30) NOT NULL,
    stdout TEXT,
    stderr TEXT,
    time_ms INT,
    memory_kb INT
);

CREATE INDEX IF NOT EXISTS idx_code_runner_verifications_revision
    ON code_runner_verifications(revision_id);
