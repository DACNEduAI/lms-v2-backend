package edu.ai.lms.core.material.app.port.out.storage;

import java.time.Duration;

public interface CreateDownloadLinkPort {

    /**
     * Creates a pre-signed download URL for the given file key with a custom expiry.
     *
     * @param fileKey the S3 object key
     * @param expiry  how long the URL remains valid (e.g. {@code Duration.ofMinutes(30)})
     * @return pre-signed download URL
     */
    String createDownloadLink(String fileKey, Duration expiry);

    /**
     * Creates a pre-signed download URL using the adapter's default expiry duration.
     *
     * @param fileKey the S3 object key
     * @return pre-signed download URL
     */
    default String createDownloadLink(String fileKey) {
        return createDownloadLink(fileKey, null);
    }
}
