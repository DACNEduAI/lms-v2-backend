BEGIN;

CREATE TABLE IF NOT EXISTS edu.assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('IMAGE', 'CODE', 'FORMULA')),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    prompt TEXT,
    purpose VARCHAR(255) NOT NULL,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (publication_status IN ('DRAFT', 'PUBLISHED')),
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (upload_status IN ('PENDING', 'UPLOADED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_assets_course_type_status ON edu.assets(course_id, type, publication_status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assets_tags ON edu.assets USING GIN(tags);

CREATE TABLE IF NOT EXISTS edu.image_assets (
    asset_id UUID PRIMARY KEY REFERENCES edu.assets(id) ON DELETE CASCADE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL CHECK (content_type IN ('image/png', 'image/jpeg', 'image/gif', 'image/svg+xml', 'image/webp')),
    byte_size BIGINT NOT NULL DEFAULT 0,
    storage_key VARCHAR(1024) NOT NULL UNIQUE
);

COMMIT;
