package edu.ai.lms.core.material.app.port.out;

public interface CreateDownloadLinkPort {

    /**
     * Creates a pre-signed download URL for the given file key.
     *
     * @param fileKey the S3 object key
     * @return pre-signed download URL
     */
    String createDownloadLink(String fileKey);
}
