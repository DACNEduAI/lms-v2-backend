package edu.ai.lms.core.quiz.adapter.out.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.material.api.PublicAssetStorageApi;

class RichContentAssetStorageAdapterUT {

    @Test
    void shouldUseOnlyTheConfiguredPublicBucket() {
        PublicAssetStorageApi storage = mock(PublicAssetStorageApi.class);
        RichContentAssetStorageAdapter adapter = new RichContentAssetStorageAdapter(storage, "lms-public");
        byte[] content = {1, 2, 3};
        when(storage.publicUrl("lms-public", "quiz2/rich-content/image.png"))
                .thenReturn("https://project.supabase.co/storage/v1/object/public/lms-public/quiz2/rich-content/image.png");
        when(storage.read("lms-public", "quiz2/rich-content/image.png")).thenReturn(content);

        adapter.upload("quiz2/rich-content/image.png", content, "image/png");
        String url = adapter.publicUrl("quiz2/rich-content/image.png");
        byte[] read = adapter.read("quiz2/rich-content/image.png");
        adapter.delete("quiz2/rich-content/image.png");

        verify(storage).upload("lms-public", "quiz2/rich-content/image.png", content, "image/png");
        verify(storage).publicUrl("lms-public", "quiz2/rich-content/image.png");
        verify(storage).read("lms-public", "quiz2/rich-content/image.png");
        verify(storage).delete("lms-public", "quiz2/rich-content/image.png");
        verifyNoMoreInteractions(storage);
        assertEquals("https://project.supabase.co/storage/v1/object/public/lms-public/quiz2/rich-content/image.png", url);
        assertEquals(content, read);
    }

    @Test
    void shouldRejectMissingPublicBucketConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new RichContentAssetStorageAdapter(mock(PublicAssetStorageApi.class), " "));
    }
}
