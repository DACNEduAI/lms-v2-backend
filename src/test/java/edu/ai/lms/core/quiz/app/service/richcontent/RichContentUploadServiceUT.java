package edu.ai.lms.core.quiz.app.service.richcontent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;

@ExtendWith(MockitoExtension.class)
class RichContentUploadServiceUT {
    @Mock RichContentAssetStoragePort storage;

    @Test
    void uploadsOnlyReferencedPngAndRewritesItsPendingSource() {
        String id = UUID.randomUUID().toString();
        when(storage.publicUrl(anyString())).thenAnswer(invocation -> "https://storage.example/" + invocation.getArgument(0));
        RichContentUploadService service = new RichContentUploadService(storage);
        MockMultipartFile image = new MockMultipartFile("images", id + ".png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});

        var prepared = service.prepare(List.of(new RichTextValue(
                "<p>x</p><img src=\"pending-image://" + id + "\" alt=\"diagram\">", RichContentFormat.TIPTAP_HTML)),
                List.of(image), List.of());

        assertTrue(prepared.values().getFirst().content().contains("https://storage.example/quiz2/rich-content/"));
        assertTrue(prepared.uploadedAssets().size() == 1);
        verify(storage).upload(anyString(), org.mockito.ArgumentMatchers.any(byte[].class), org.mockito.ArgumentMatchers.eq("image/png"));
    }

    @Test
    void rejectsExternalImageAndMaliciousSvg() {
        RichContentUploadService service = new RichContentUploadService(storage);
        assertThrows(RichContentValidationException.class, () -> service.prepare(List.of(new RichTextValue(
                "<img src=\"https://untrusted.example/image.png\">", RichContentFormat.TIPTAP_HTML)), List.of(), List.of()));

        String id = UUID.randomUUID().toString();
        MockMultipartFile unsafeSvg = new MockMultipartFile("images", id + ".svg", "image/svg+xml",
                "<svg onload=\"alert(1)\"></svg>".getBytes());
        assertThrows(RichContentValidationException.class, () -> service.prepare(List.of(new RichTextValue(
                "<img src=\"pending-image://" + id + "\">", RichContentFormat.TIPTAP_HTML)), List.of(unsafeSvg), List.of()));
    }

    @Test
    void findsReferencedStorageKeysFromSingleOrDoubleQuotedImageSources() {
        when(storage.publicUrl("quiz2/rich-content/existing.png"))
                .thenReturn("https://storage.example/quiz2/rich-content/existing.png");
        RichContentUploadService service = new RichContentUploadService(storage);
        RichContentAsset existing = new RichContentAsset(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "quiz2/rich-content/existing.png", "image/png", 1, "hash", null, null);

        Set<String> referenced = service.referencedStorageKeys(List.of(new RichTextValue(
                "<p><img src='https://storage.example/quiz2/rich-content/existing.png'></p>",
                RichContentFormat.TIPTAP_HTML)), List.of(existing), List.of());

        assertTrue(referenced.contains("quiz2/rich-content/existing.png"));
    }
}
