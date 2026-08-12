package edu.ai.lms.core.material.adapter.out.storage;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import edu.ai.lms.core.material.adapter.out.storage.config.StorageConfig;
import edu.ai.lms.core.material.app.port.out.storage.CreateDownloadLinkPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateUploadLinkPort;
import edu.ai.lms.core.material.app.port.out.storage.DeleteFilePort;
import edu.ai.lms.core.material.app.port.out.storage.GetPublicFileUrlPort;
import edu.ai.lms.core.material.app.port.out.storage.UploadFilePort;
import edu.ai.lms.core.material.app.port.out.storage.VerifyFileUploadedPort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Facade that delegates storage operations to the appropriate S3Client:
 *  - "prod" profile → DigitalOceanConfig's S3Client
 *  - "dev"  profile → DevConfig's S3-compatible client (Supabase Storage locally)
 *
 * Spring automatically injects the profile-active bean; this class adds
 * helper methods and a uniform API on top of it.
 */
@Service
public class S3StorageAdapter implements CreateDownloadLinkPort, CreateUploadLinkPort, DeleteFilePort,
    VerifyFileUploadedPort, UploadFilePort, GetPublicFileUrlPort {

    private static final Duration DEFAULT_PRESIGN_EXPIRY = Duration.ofMinutes(5);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final Environment environment;
    private final String bucketName;
    private final String storageEndpoint;

    public S3StorageAdapter(StorageConfig storageConfig, Environment environment) {
        this.s3Client = storageConfig.s3Client();
        this.s3Presigner = storageConfig.s3Presigner();
        this.environment = environment;
        this.bucketName = storageConfig.getBucketName();
        this.storageEndpoint = storageConfig.getEndPoint();
    }

    // Profile helpers

    public boolean isUsingDigitalOcean() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    public boolean isUsingLocalStorage() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    // Storage operations

    /**
     * Deletes an object from the given bucket.
     *
     * @param bucketName source bucket
     * @param key        object key to delete
     */
    public void deleteFile(String bucketName, String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    /**
     * Lists all file keys in a bucket that match a specific prefix (folder).
     * For example, passing prefix "course-1/" will return ["course-1/doc1.txt", "course-1/doc2.txt"].
     *
     * @param bucketName source bucket
     * @param prefix     the folder path or key prefix (e.g., "course-1/")
     * @return List of matching object keys
     */
    public List<String> listFilesByPrefix(String bucketName, String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        // Extract just the keys (file paths) from the returned S3 objects
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Verifies that a file exists in the bucket by issuing a HeadObject request.
     * Returns {@code true} if the object is present, {@code false} if it does not exist.
     *
     * @param bucketName source bucket
     * @param key        object key to check
     * @return {@code true} if the object exists, {@code false} otherwise
     */
    public boolean verifyFileUploaded(String bucketName, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Creates a pre-signed URL that allows a client to upload (PUT) a file directly
     * to S3 without requiring AWS credentials on their side.
     *
     * @param bucketName  target bucket
     * @param key         object key under which the file will be stored
     * @param contentType MIME type the client must send in the Content-Type header
     * @param expiry      how long the URL remains valid (e.g. {@code Duration.ofMinutes(15)})
     * @return pre-signed PUT URL string
     */
    public String createPresignedUploadUrl(String bucketName,
                                           String key,
                                           String contentType,
                                           Duration expiry) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toExternalForm();
    }

    /**
     * Creates a pre-signed URL for downloading a file with a configurable expiry.
     *
     * @param bucketName target bucket
     * @param key        object key
     * @param expiry     how long the URL should remain valid (e.g. Duration.ofHours(1))
     * @return pre-signed URL string
     */
    public String createPresignedDownloadUrl(String bucketName, String key, Duration expiry) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toExternalForm();
    }

    /**
     * Uploads object content directly to storage.
     */
    public void uploadFile(String bucketName, String key, byte[] content, String contentType, boolean isPublic) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key);

        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.contentType(contentType);
        }

        if (isPublic) {
            requestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(content));
    }

    /**
     * Reads an object directly from storage. Callers must already own and
     * authorize the referenced key; this method never accepts a public URL.
     */
    public byte[] readFile(String bucketName, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    /**
     * Builds a public URL for object access. This URL is only usable for publicly
     * readable objects.
     */
    public String getPublicUrl(String bucketName, String key) {
        if (storageEndpoint == null || storageEndpoint.isBlank()) {
            throw new IllegalStateException("Storage endpoint is not configured");
        }

        URI endpointUri = URI.create(storageEndpoint);
        String scheme = endpointUri.getScheme() != null ? endpointUri.getScheme() : "https";
        String authority = endpointUri.getAuthority();

        if (authority == null || authority.isBlank()) {
            throw new IllegalStateException("Storage endpoint authority is invalid");
        }

        String normalizedEndpoint = removeTrailingSlash(storageEndpoint);
        String host = endpointUri.getHost();
        String encodedKey = encodePathSegments(key);

        if (host != null && host.endsWith(".storage.supabase.co")) {
            String publicHost = host.substring(0, host.length() - ".storage.supabase.co".length())
                    + ".supabase.co";
            return scheme + "://" + publicHost + "/storage/v1/object/public/"
                    + encodePathSegment(bucketName) + "/" + encodedKey;
        }

        if (authority.startsWith(bucketName + ".")) {
            return normalizedEndpoint + "/" + encodedKey;
        }

        return scheme + "://" + bucketName + "." + authority + "/" + encodedKey;
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String encodePathSegments(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        return Arrays.stream(key.split("/"))
                .map(this::encodePathSegment)
                .collect(Collectors.joining("/"));
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ── Port implementations ────────────────────────────────────────────

    @Override
    public String createDownloadLink(String fileKey, Duration expiry) {
        return createPresignedDownloadUrl(bucketName, fileKey,
                expiry != null ? expiry : DEFAULT_PRESIGN_EXPIRY);
    }

    @Override
    public String createUploadLink(String fileKey, String contentType, Duration expiry) {
        return createPresignedUploadUrl(bucketName, fileKey, contentType,
                expiry != null ? expiry : DEFAULT_PRESIGN_EXPIRY);
    }

    @Override
    public void deleteFile(String fileKey) {
        deleteFile(bucketName, fileKey);
    }

    @Override
    public boolean isFileUploaded(String fileKey) {
        return verifyFileUploaded(bucketName, fileKey);
    }

    @Override
    public void uploadFile(String fileKey, byte[] content, String contentType, boolean isPublic) {
        uploadFile(bucketName, fileKey, content, contentType, isPublic);
    }

    @Override
    public String getPublicFileUrl(String fileKey) {
        return getPublicUrl(bucketName, fileKey);
    }
}
