ALTER TABLE edu.quiz_generation_jobs
    ADD COLUMN IF NOT EXISTS generated_question_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_question_count INTEGER NOT NULL DEFAULT 0;
