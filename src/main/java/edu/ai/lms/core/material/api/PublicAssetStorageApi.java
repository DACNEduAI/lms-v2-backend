package edu.ai.lms.core.material.api;

/**
 * Bucket-explicit storage operations for durable public assets owned by
 * another module. Course materials continue to use the private bucket APIs.
 */
public interface PublicAssetStorageApi {
    void upload(String bucketName, String storageKey, byte[] content, String mediaType);

    String publicUrl(String bucketName, String storageKey);

    byte[] read(String bucketName, String storageKey);

    void delete(String bucketName, String storageKey);
}
