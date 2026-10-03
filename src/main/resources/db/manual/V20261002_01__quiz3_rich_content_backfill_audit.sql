BEGIN;

CREATE TABLE IF NOT EXISTS edu.quiz3_rich_content_backfill_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id UUID NOT NULL,
    entity_type VARCHAR(30) NOT NULL CHECK(entity_type IN('QUESTION','ANSWER','ATTEMPT_RECORD')),
    field_name VARCHAR(40) NOT NULL,
    question_id UUID NOT NULL,
    answer_number BIGINT,
    attempt_id UUID,
    before_content TEXT,
    before_format VARCHAR(40),
    after_content TEXT NOT NULL,
    after_format VARCHAR(40) NOT NULL,
    restored_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quiz3_rich_content_backfill_run
    ON edu.quiz3_rich_content_backfill_audit(run_id,id);

COMMIT;
