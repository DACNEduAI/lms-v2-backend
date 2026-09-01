CREATE INDEX IF NOT EXISTS idx_code_submissions_cooldown_lookup
ON edu.code_submissions (attempt_id, question_id, submission_mode, accepted_at DESC);
