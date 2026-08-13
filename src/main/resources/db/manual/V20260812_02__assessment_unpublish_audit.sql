ALTER TABLE edu.assessments
    ADD COLUMN IF NOT EXISTS last_unpublished_by UUID REFERENCES edu.users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS last_unpublished_at TIMESTAMP;
