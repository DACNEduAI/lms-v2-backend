package edu.ai.lms.core.material.app.port.out.storage;

public interface CreateUploadLinkPort {

    /**
     * Creates a pre-signed upload URL for the given file key.
     *
     * @param fileKey     the S3 object key
     * @param contentType MIME type the client must send (e.g. "application/pdf")
     * @return pre-signed upload URL
     */
    String createUploadLink(String fileKey, String contentType);
}
