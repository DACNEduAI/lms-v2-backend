package edu.ai.lms.core.material.api;

/**
 * Storage operations for course-owned assets from modules other than Material.
 * The caller owns authorization and the object key; this API deliberately does
 * not expose S3 clients or bucket configuration.
 */
public interface CourseAssetStorageApi {
    String createUploadLink(String storageKey, String contentType);
    String createDownloadLink(String storageKey);
    StoredObjectMetadata inspect(String storageKey);
    byte[] read(String storageKey);
    void delete(String storageKey);
}
