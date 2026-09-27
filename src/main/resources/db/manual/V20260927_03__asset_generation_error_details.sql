BEGIN;

ALTER TABLE edu.asset_generation_jobs
    ADD COLUMN IF NOT EXISTS error_details TEXT;

COMMIT;
