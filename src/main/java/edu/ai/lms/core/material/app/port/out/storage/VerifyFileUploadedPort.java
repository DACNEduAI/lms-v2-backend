package edu.ai.lms.core.material.app.port.out.storage;

public interface VerifyFileUploadedPort {

    /**
     * Checks whether a file exists in storage for the given key.
     *
     * @param fileKey the S3 object key to verify
     * @return {@code true} if the file exists, {@code false} otherwise
     */
    boolean isFileUploaded(String fileKey);
}
