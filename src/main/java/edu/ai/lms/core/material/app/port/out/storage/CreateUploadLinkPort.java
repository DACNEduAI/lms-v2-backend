package edu.ai.lms.core.material.app.port.out.storage;

import java.time.Duration;

public interface CreateUploadLinkPort {

    /**
     * Creates a pre-signed upload URL for the given file key with a custom expiry.
     *
     * @param fileKey     the S3 object key
     * @param contentType MIME type the client must send (e.g. "application/pdf")
     * @param expiry      how long the URL remains valid (e.g. {@code Duration.ofMinutes(30)})
     * @return pre-signed upload URL
     */
    String createUploadLink(String fileKey, String contentType, Duration expiry);

    /**
     * Creates a pre-signed upload URL using the adapter's default expiry duration.
     *
     * @param fileKey     the S3 object key
     * @param contentType MIME type the client must send (e.g. "application/pdf")
     * @return pre-signed upload URL
     */
    default String createUploadLink(String fileKey, String contentType) {
        return createUploadLink(fileKey, contentType, null);
    }
}
