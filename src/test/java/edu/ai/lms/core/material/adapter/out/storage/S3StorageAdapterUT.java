package edu.ai.lms.core.material.adapter.out.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import edu.ai.lms.core.material.adapter.out.storage.config.StorageConfig;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3StorageAdapterUT {

    @Test
    void shouldBuildSupabasePublicObjectUrl() {
        S3StorageAdapter storage = storage("https://project-ref.storage.supabase.co/storage/v1/s3");

        assertEquals(
                "https://project-ref.supabase.co/storage/v1/object/public/lms-public/quiz2/rich-content/diagram%201.png",
                storage.getPublicUrl("lms-public", "quiz2/rich-content/diagram 1.png"));
    }

    @Test
    void shouldKeepVirtualHostUrlForNonSupabaseStorage() {
        S3StorageAdapter storage = storage("https://sgp1.digitaloceanspaces.com");

        assertEquals(
                "https://lms-public.sgp1.digitaloceanspaces.com/quiz2/rich-content/diagram.png",
                storage.getPublicUrl("lms-public", "quiz2/rich-content/diagram.png"));
    }

    private S3StorageAdapter storage(String endpoint) {
        StorageConfig config = mock(StorageConfig.class);
        when(config.s3Client()).thenReturn(mock(S3Client.class));
        when(config.s3Presigner()).thenReturn(mock(S3Presigner.class));
        when(config.getBucketName()).thenReturn("lms-materials");
        when(config.getEndPoint()).thenReturn(endpoint);
        return new S3StorageAdapter(config, mock(Environment.class));
    }
}
