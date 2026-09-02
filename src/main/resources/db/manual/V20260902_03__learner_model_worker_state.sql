CREATE TABLE IF NOT EXISTS edu.learner_model_processed_events (
 learning_event_id UUID NOT NULL REFERENCES edu.learning_events(id) ON DELETE CASCADE, model_name VARCHAR(80) NOT NULL, model_version VARCHAR(40) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(learning_event_id,model_name,model_version));
CREATE TABLE IF NOT EXISTS edu.learner_model_workers (
 worker_id VARCHAR(100) PRIMARY KEY, worker_version VARCHAR(40) NOT NULL, started_at TIMESTAMPTZ NOT NULL, last_heartbeat_at TIMESTAMPTZ NOT NULL, current_job_id UUID REFERENCES edu.learner_model_jobs(id) ON DELETE SET NULL, status VARCHAR(20) NOT NULL CHECK(status IN('RUNNING','STOPPED')));
