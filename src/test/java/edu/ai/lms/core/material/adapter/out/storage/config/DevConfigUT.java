package edu.ai.lms.core.material.adapter.out.storage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.net.URI;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class DevConfigUT {

    @Test
    void shouldCreatePathStyleSupabaseClientAndPresignedUploadUrl() throws Exception {
        DevConfig config = new DevConfig();
        config.setRegion("ap-southeast-1");
        config.setAccessKey("test-access-key");
        config.setSecretKey("test-secret-key");
        config.setEndPoint("https://project.storage.supabase.co/storage/v1/s3");
        config.setBucketName("private-materials");
        config.setForcePathStyle(true);

        try (S3Client client = config.s3Client(); S3Presigner presigner = config.s3Presigner()) {
            assertNotNull(client);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key("courses/course-id/lecture.pdf")
                    .contentType("application/pdf")
                    .build();
            URI presignedUri = presigner.presignPutObject(PutObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(5))
                            .putObjectRequest(putObjectRequest)
                            .build())
                    .url()
                    .toURI();

            assertEquals("project.storage.supabase.co", presignedUri.getHost());
            assertEquals(
                    "/storage/v1/s3/private-materials/courses/course-id/lecture.pdf",
                    presignedUri.getPath());
            assertTrue(presignedUri.getRawQuery().contains("ap-southeast-1%2Fs3%2Faws4_request"));
            assertTrue(presignedUri.getRawQuery().contains("X-Amz-SignedHeaders=content-type%3Bhost"));
        }
    }
}
