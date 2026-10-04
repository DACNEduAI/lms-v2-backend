package edu.ai.lms.core.quiz3.app.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;
import edu.ai.lms.core.quiz3.app.domain.Quiz3Models;
import edu.ai.lms.core.quiz3.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz3.app.port.out.RichContentAssetStoragePort;

class Quiz3RichContentServiceUT {
    @Test
    void rejectsDraftCourseAssetBeforeSnapshotting() {
        UUID courseId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        AssetRepository courseAssets = mock(AssetRepository.class);
        Quiz3RichContentAssetRepository richAssets = mock(Quiz3RichContentAssetRepository.class);
        when(richAssets.findByQuestionId(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(courseAssets.findImage(courseId, assetId)).thenReturn(Optional.of(image(courseId, assetId, Asset.PublicationStatus.DRAFT, Asset.UploadStatus.UPLOADED)));
        var service = new Quiz3RichContentService(mock(Quiz3AssessmentService.class), richAssets,
                new Quiz3RichContentUploadService(mock(RichContentAssetStoragePort.class)), mock(Quiz3RichContentNormalizer.class),
                courseAssets, mock(CourseAssetStorageApi.class), mock(CourseTeacherAccess.class));
        var request = new Quiz3Models.QuestionUpdate(1, "<img src=\"course-asset-image://" + token + "\">", "TIPTAP_HTML",
                null, "TIPTAP_HTML", List.of(), List.of(new Quiz3Models.ImageAssetSelection(token.toString(), assetId)));

        assertThatThrownBy(() -> service.update(courseId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), request, List.of()))
                .isInstanceOf(RichContentValidationException.class)
                .hasMessageContaining("published and uploaded");
    }

    @Test
    void rejectsAssetFromAnotherCourseAsUnavailable() {
        UUID courseId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        AssetRepository courseAssets = mock(AssetRepository.class);
        Quiz3RichContentAssetRepository richAssets = mock(Quiz3RichContentAssetRepository.class);
        when(richAssets.findByQuestionId(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(courseAssets.findImage(courseId, assetId)).thenReturn(Optional.empty());
        var service = new Quiz3RichContentService(mock(Quiz3AssessmentService.class), richAssets,
                new Quiz3RichContentUploadService(mock(RichContentAssetStoragePort.class)), mock(Quiz3RichContentNormalizer.class),
                courseAssets, mock(CourseAssetStorageApi.class), mock(CourseTeacherAccess.class));
        var request = new Quiz3Models.QuestionUpdate(1, "<img src=\"course-asset-image://" + token + "\">", "TIPTAP_HTML",
                null, "TIPTAP_HTML", List.of(), List.of(new Quiz3Models.ImageAssetSelection(token.toString(), assetId)));

        assertThatThrownBy(() -> service.update(courseId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), request, List.of()))
                .isInstanceOf(RichContentValidationException.class)
                .hasMessageContaining("unavailable");
    }

    private static ImageAsset image(UUID courseId, UUID assetId, Asset.PublicationStatus publication, Asset.UploadStatus upload) {
        LocalDateTime now = LocalDateTime.now();
        Asset asset = new Asset(assetId, courseId, Asset.Type.IMAGE, "Tree", "Tree diagram", null, List.of(), publication, upload, now, now);
        return new ImageAsset(asset, "tree.png", "image/png", 8, "courses/tree.png", null, false);
    }
}
