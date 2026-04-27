package edu.ai.lms.core.material.app.port.out.storage;

public interface GetPublicFileUrlPort {

    /**
     * Returns the public URL for a file key.
     *
     * @param fileKey object key in storage
     * @return public URL
     */
    String getPublicFileUrl(String fileKey);
}
