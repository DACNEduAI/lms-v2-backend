package edu.ai.lms.core.material.api;

import java.time.Duration;

/**
 * Public Material-module boundary for private, short-lived processing files.
 */
public interface TemporaryStorageApi {
    void upload(String fileKey, byte[] content, String contentType);

    String createDownloadLink(String fileKey, Duration expiry);

    void delete(String fileKey);
}
