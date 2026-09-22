package edu.ai.lms.core.material.api;

/** Metadata returned after a direct browser upload. */
public record StoredObjectMetadata(boolean exists, long contentLength, String contentType) {
    public static StoredObjectMetadata missing() { return new StoredObjectMetadata(false, 0, null); }
}
