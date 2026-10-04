package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.Quiz3Models;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz3.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz3.app.port.out.RichContentAssetStoragePort;

class Quiz3RichContentUploadServiceUT {
    @Test
    void storesAiSvgUnderQuiz3Namespace() {
        RichContentAssetStoragePort storage = mock(RichContentAssetStoragePort.class);
        when(storage.publicUrl(any())).thenAnswer(call -> "https://assets/" + call.getArgument(0));
        var uploaded = new Quiz3RichContentUploadService(storage).uploadAiSvg("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><path d=\"M0 0\"/></svg>");
        assertThat(uploaded.storageKey()).startsWith("quiz3/rich-content/").endsWith(".svg");
        assertThat(uploaded.publicUrl()).contains("quiz3/rich-content/");
    }

    @Test
    void rejectsUnsafeSvgBeforeStorage() {
        RichContentAssetStoragePort storage = mock(RichContentAssetStoragePort.class);
        assertThatThrownBy(() -> new Quiz3RichContentUploadService(storage).uploadAiSvg("<svg><script>alert(1)</script></svg>"))
                .isInstanceOf(RichContentValidationException.class);
    }

    @Test
    void snapshotsSelectedCourseAssetsAndRewritesEveryToken() {
        RichContentAssetStoragePort storage = mock(RichContentAssetStoragePort.class);
        when(storage.publicUrl(any())).thenAnswer(call -> "https://assets/" + call.getArgument(0));
        var service = new Quiz3RichContentUploadService(storage);
        UUID assetId = UUID.randomUUID();
        UUID firstToken = UUID.randomUUID();
        UUID secondToken = UUID.randomUUID();
        String html = "<p><img src=\"course-asset-image://" + firstToken + "\"><img src=\"course-asset-image://" + secondToken + "\"></p>";

        var prepared = service.prepare(List.of(new RichTextValue(html, RichContentFormat.TIPTAP_HTML)), List.of(), List.of(),
                List.of(new Quiz3Models.ImageAssetSelection(firstToken.toString(), assetId),
                        new Quiz3Models.ImageAssetSelection(secondToken.toString(), assetId)),
                id -> new Quiz3RichContentUploadService.CourseAssetSource(id, png(), "image/png"));

        assertThat(prepared.values().getFirst().content()).doesNotContain("course-asset-image://").contains("https://assets/quiz3/rich-content/");
        assertThat(prepared.uploadedAssets()).hasSize(1);
        assertThat(prepared.uploadedAssets().getFirst().sourceAssetId()).isEqualTo(assetId);
        verify(storage, times(1)).upload(any(), any(), any());
    }

    @Test
    void rejectsMissingAndDuplicateCourseAssetSelections() {
        RichContentAssetStoragePort storage = mock(RichContentAssetStoragePort.class);
        var service = new Quiz3RichContentUploadService(storage);
        UUID assetId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        var values = List.of(new RichTextValue("<img src=\"course-asset-image://" + token + "\">", RichContentFormat.TIPTAP_HTML));

        assertThatThrownBy(() -> service.prepare(values, List.of(), List.of(), List.of(),
                id -> new Quiz3RichContentUploadService.CourseAssetSource(id, png(), "image/png")))
                .isInstanceOf(RichContentValidationException.class)
                .hasMessageContaining("referenced exactly once");
        assertThatThrownBy(() -> service.prepare(values, List.of(), List.of(),
                List.of(new Quiz3Models.ImageAssetSelection(token.toString(), assetId),
                        new Quiz3Models.ImageAssetSelection(token.toString(), assetId)),
                id -> new Quiz3RichContentUploadService.CourseAssetSource(id, png(), "image/png")))
                .isInstanceOf(RichContentValidationException.class)
                .hasMessageContaining("Duplicate");
        var duplicateReference = List.of(new RichTextValue("<img src=\"course-asset-image://" + token
                + "\"><img src=\"course-asset-image://" + token + "\">", RichContentFormat.TIPTAP_HTML));
        assertThatThrownBy(() -> service.prepare(duplicateReference, List.of(), List.of(),
                List.of(new Quiz3Models.ImageAssetSelection(token.toString(), assetId)),
                id -> new Quiz3RichContentUploadService.CourseAssetSource(id, png(), "image/png")))
                .isInstanceOf(RichContentValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    private static byte[] png() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    }
}
