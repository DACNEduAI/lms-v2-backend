-- Persistent Qdrant Cloud registry. This schema is intentionally not part of
-- the resettable edu schema used by schema.sql/data.sql development fixtures.
CREATE SCHEMA IF NOT EXISTS ai_index;

CREATE TABLE IF NOT EXISTS ai_index.document_indexes
(
    bucket_name     VARCHAR(255) NOT NULL,
    file_key        VARCHAR(1024) NOT NULL,
    index_profile   VARCHAR(255) NOT NULL,
    source_hash     VARCHAR(64) NOT NULL,
    collection_name VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('READY', 'FAILED')),
    chunk_count     INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    indexed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bucket_name, file_key, index_profile)
);

CREATE INDEX IF NOT EXISTS idx_document_indexes_ready
    ON ai_index.document_indexes (status, index_profile);
