CREATE TABLE IF NOT EXISTS edu.learning_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), observation_key VARCHAR(255) NOT NULL,
    observation_revision INTEGER NOT NULL CHECK (observation_revision > 0),
    student_id UUID NOT NULL REFERENCES edu.students(user_id), course_id UUID NOT NULL REFERENCES edu.courses(id),
    quiz_id UUID NOT NULL REFERENCES edu.quizzes(id), attempt_id UUID NOT NULL REFERENCES edu.attempts(id),
    question_id UUID NOT NULL REFERENCES edu.questions(id), question_version BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL CHECK (source_type IN ('MULTIPLE_CHOICE', 'CODE_RUNNER')),
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN ('GRADED_RESPONSE_FINALIZED', 'GRADED_RESPONSE_REGRADED')),
    occurred_at TIMESTAMP NOT NULL, is_correct BOOLEAN, score_ratio DOUBLE PRECISION,
    mastery_eligible BOOLEAN NOT NULL, supersedes_event_id UUID REFERENCES edu.learning_events(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (observation_key, observation_revision)
);
CREATE INDEX IF NOT EXISTS idx_learning_events_student_course_time ON edu.learning_events(student_id, course_id, occurred_at, id);
CREATE TABLE IF NOT EXISTS edu.learning_event_learning_outcomes (
    learning_event_id UUID NOT NULL REFERENCES edu.learning_events(id) ON DELETE CASCADE,
    learning_outcome_id UUID NOT NULL REFERENCES edu.learning_outcomes(id) ON DELETE RESTRICT,
    PRIMARY KEY (learning_event_id, learning_outcome_id)
);
CREATE TABLE IF NOT EXISTS edu.learner_model_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), dedupe_key VARCHAR(255) NOT NULL UNIQUE,
    job_type VARCHAR(40) NOT NULL CHECK (job_type = 'REBUILD_STUDENT_COURSE'),
    student_id UUID NOT NULL REFERENCES edu.students(user_id), course_id UUID NOT NULL REFERENCES edu.courses(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ, lease_until TIMESTAMPTZ,
    worker_id VARCHAR(100), error_message TEXT, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_learner_model_jobs_queue ON edu.learner_model_jobs(status, next_attempt_at);
