BEGIN;

ALTER TABLE edu.image_assets
    ADD COLUMN IF NOT EXISTS pending_original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pending_content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pending_storage_key VARCHAR(1024);

ALTER TABLE edu.image_assets
    DROP CONSTRAINT IF EXISTS image_assets_pending_content_type_check;
ALTER TABLE edu.image_assets
    ADD CONSTRAINT image_assets_pending_content_type_check
        CHECK (pending_content_type IS NULL OR pending_content_type IN ('image/png', 'image/jpeg', 'image/gif', 'image/svg+xml', 'image/webp'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_image_assets_pending_storage_key
    ON edu.image_assets(pending_storage_key) WHERE pending_storage_key IS NOT NULL;

COMMIT;
