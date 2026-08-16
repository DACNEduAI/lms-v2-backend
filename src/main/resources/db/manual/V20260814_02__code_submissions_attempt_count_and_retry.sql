ALTER TABLE edu.code_submissions ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE edu.code_submissions ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP;
