package edu.ai.lms.core.material.app.port.out.storage;

public interface UploadFilePort {

    /**
     * Uploads a file directly to storage.
     *
     * @param fileKey     object key in storage
     * @param content     raw file content
     * @param contentType MIME type of the file
     * @param isPublic    whether uploaded object should be publicly readable
     */
    void uploadFile(String fileKey, byte[] content, String contentType, boolean isPublic);

    /**
     * Uploads a private file directly to storage.
     *
     * @param fileKey     object key in storage
     * @param content     raw file content
     * @param contentType MIME type of the file
     */
    default void uploadFile(String fileKey, byte[] content, String contentType) {
        uploadFile(fileKey, content, contentType, false);
    }
}
