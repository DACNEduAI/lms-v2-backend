package edu.ai.lms.core.material.adapter.in.internal;

/**
 * Public API exposed by the Material module for direct storage uploads.
 */
public interface UploadStorageFileApi {

    /**
     * Uploads content directly to storage with optional public visibility.
     *
     * @param fileKey     object key in storage
     * @param content     file bytes
     * @param contentType MIME type of the content
     * @param isPublic    whether the file should be publicly readable
     */
    void uploadFile(String fileKey, byte[] content, String contentType, boolean isPublic);
}
