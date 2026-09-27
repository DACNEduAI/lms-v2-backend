BEGIN;

CREATE TABLE IF NOT EXISTS edu.asset_generation_jobs (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL UNIQUE REFERENCES edu.assets(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
    requested_by UUID,
    prompt TEXT NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'READY_FOR_REVIEW', 'FAILED')),
    attempt INTEGER NOT NULL DEFAULT 1,
    callback_event_id UUID,
    error_message VARCHAR(1000),
    tikz_source TEXT,
    model VARCHAR(255),
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_asset_generation_jobs_queue
    ON edu.asset_generation_jobs (status, created_at);

COMMIT;
