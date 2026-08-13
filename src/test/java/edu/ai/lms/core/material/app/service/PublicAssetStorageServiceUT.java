package edu.ai.lms.core.material.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.material.adapter.out.storage.S3StorageAdapter;

class PublicAssetStorageServiceUT {

    @Test
    void shouldDelegateBucketExplicitPublicOperations() {
        S3StorageAdapter storage = mock(S3StorageAdapter.class);
        PublicAssetStorageService service = new PublicAssetStorageService(storage);
        byte[] content = {1, 2, 3};
        when(storage.getPublicUrl("lms-public", "quiz2/rich-content/image.png"))
                .thenReturn("https://project.supabase.co/storage/v1/object/public/lms-public/quiz2/rich-content/image.png");
        when(storage.readFile("lms-public", "quiz2/rich-content/image.png")).thenReturn(content);

        service.upload("lms-public", "quiz2/rich-content/image.png", content, "image/png");
        String url = service.publicUrl("lms-public", "quiz2/rich-content/image.png");
        byte[] read = service.read("lms-public", "quiz2/rich-content/image.png");
        service.delete("lms-public", "quiz2/rich-content/image.png");

        verify(storage).uploadFile("lms-public", "quiz2/rich-content/image.png", content, "image/png", true);
        verify(storage).getPublicUrl("lms-public", "quiz2/rich-content/image.png");
        verify(storage).readFile("lms-public", "quiz2/rich-content/image.png");
        verify(storage).deleteFile("lms-public", "quiz2/rich-content/image.png");
        assertEquals("https://project.supabase.co/storage/v1/object/public/lms-public/quiz2/rich-content/image.png", url);
        assertEquals(content, read);
    }
}
