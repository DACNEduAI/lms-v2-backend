package edu.ai.lms.core.material.app.port.out.storage;

public interface DeleteFilePort {

    /**
     * Deletes the file identified by the given key from storage.
     *
     * @param fileKey the S3 object key to delete
     */
    void deleteFile(String fileKey);
}
