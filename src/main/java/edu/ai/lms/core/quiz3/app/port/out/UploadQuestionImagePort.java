package edu.ai.lms.core.quiz3.app.port.out;

public interface UploadQuestionImagePort {

    /**
     * Uploads a question image into storage.
     *
     * @param fileKey     object key in storage
     * @param content     image bytes
     * @param contentType image MIME type
     * @param isPublic    whether uploaded object should be publicly readable
     */
    void uploadQuestionImage(String fileKey, byte[] content, String contentType, boolean isPublic);
}
