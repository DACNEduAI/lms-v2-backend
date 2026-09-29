package edu.ai.lms.core.asset.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.exception.AssetValidationException;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageAssetServiceUT {
    @Mock private AssetRepository assets;
    @Mock private CourseTeacherAccess access;
    @Mock private CourseAssetStorageApi storage;
    @Mock private AssetVectorSyncService vectors;
    private ImageAssetService service;
    private UUID courseId;
    private UUID assetId;

    @BeforeEach void setUp() {
        service = new ImageAssetService(assets, access, storage, vectors);
        courseId = UUID.randomUUID(); assetId = UUID.randomUUID();
        lenient().when(assets.saveImage(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void updatesDraftMetadata() {
        ImageAsset original = image(Asset.PublicationStatus.DRAFT, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(original));

        ImageAsset updated = service.update(courseId, assetId,
                new ImageAssetService.UpdateImage("New title", "Updated description", "  prompt  ", List.of("Graph", "graph", " ")));

        assertThat(updated.asset().name()).isEqualTo("New title");
        assertThat(updated.asset().publicationStatus()).isEqualTo(Asset.PublicationStatus.DRAFT);
        assertThat(updated.asset().uploadStatus()).isEqualTo(Asset.UploadStatus.UPLOADED);
        assertThat(updated.asset().tags()).containsExactly("graph");
        assertThat(updated.storageKey()).isEqualTo("courses/source.png");
    }

    @Test void rejectsUpdatesToPublishedImagesUntilTheyAreUnpublished() {
        ImageAsset published = image(Asset.PublicationStatus.PUBLISHED, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.update(courseId, assetId,
                new ImageAssetService.UpdateImage("New title", "Updated description", null, List.of())))
                .isInstanceOf(AssetValidationException.class)
                .hasMessage("Unpublish the image asset before editing it");
    }

    @Test void acceptsProseMetadataUpToFiveThousandCharacters() {
        ImageAsset original = image(Asset.PublicationStatus.DRAFT, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(original));
        String prose = "x".repeat(5000);

        ImageAsset updated = service.update(courseId, assetId,
                new ImageAssetService.UpdateImage("Title", prose, prose, List.of()));

        assertThat(updated.asset().description()).isEqualTo(prose);
        assertThat(updated.asset().prompt()).isEqualTo(prose);
    }

    @Test void rejectsProseMetadataOverFiveThousandCharacters() {
        String tooLong = "x".repeat(5001);

        assertThatThrownBy(() -> service.update(courseId, assetId,
                new ImageAssetService.UpdateImage("Title", tooLong, "Prompt", List.of())))
                .isInstanceOf(AssetValidationException.class);
        assertThatThrownBy(() -> service.update(courseId, assetId,
                new ImageAssetService.UpdateImage("Title", "Description", tooLong, List.of())))
                .isInstanceOf(AssetValidationException.class);
    }

    @Test void rejectsReplacementOperationsForEveryImageAsset() {
        assertThatThrownBy(() -> service.beginReplacement(courseId, assetId,
                new ImageAssetService.ReplacementImage("replacement.png", "image/png")))
                .isInstanceOf(AssetValidationException.class)
                .hasMessage("Image assets cannot be replaced; create a new image asset instead");
        assertThatThrownBy(() -> service.confirmReplacement(courseId, assetId))
                .isInstanceOf(AssetValidationException.class)
                .hasMessage("Image assets cannot be replaced; create a new image asset instead");
    }

    @Test void createsUploadWithNullPromptAndPendingMetadata() {
        when(assets.createImage(any())).thenAnswer(call -> call.getArgument(0));
        when(storage.createUploadLink(any(), eq("image/png"))).thenReturn("https://upload");

        ImageAssetService.UploadCreated created = service.createUpload(courseId,
                new ImageAssetService.UploadImage("diagram.png", "image/png"));

        assertThat(created.image().asset().prompt()).isNull();
        assertThat(created.image().asset().name()).isEqualTo("Analyzing uploaded image");
        assertThat(created.image().asset().uploadStatus()).isEqualTo(Asset.UploadStatus.PENDING);
        assertThat(created.presignedUploadUrl()).isEqualTo("https://upload");
    }

    @Test void publishesOnlyAfterTheAssetVectorIsIndexed() {
        ImageAsset draft = image(Asset.PublicationStatus.DRAFT, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(draft));

        ImageAsset published = service.setPublication(courseId, assetId, Asset.PublicationStatus.PUBLISHED);

        assertThat(published.asset().publicationStatus()).isEqualTo(Asset.PublicationStatus.PUBLISHED);
        verify(vectors).upsert(published);
        verify(assets).saveImage(published);
    }

    @Test void doesNotPublishWhenAssetVectorIndexingFails() {
        ImageAsset draft = image(Asset.PublicationStatus.DRAFT, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(draft));
        org.mockito.Mockito.doThrow(new RuntimeException("pipeline unavailable")).when(vectors).upsert(any());

        assertThatThrownBy(() -> service.setPublication(courseId, assetId, Asset.PublicationStatus.PUBLISHED))
                .isInstanceOf(RuntimeException.class).hasMessage("pipeline unavailable");
        verify(assets, never()).saveImage(any());
    }

    @Test void unpublishingAndDeletingRemoveTheAssetVector() {
        ImageAsset published = image(Asset.PublicationStatus.PUBLISHED, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(published));

        service.setPublication(courseId, assetId, Asset.PublicationStatus.DRAFT);
        service.delete(courseId, assetId);

        verify(vectors, org.mockito.Mockito.times(2)).delete(assetId);
        verify(storage).delete("courses/source.png");
    }

    @Test void republishingAnAssetResynchronizesItsExistingVector() {
        ImageAsset published = image(Asset.PublicationStatus.PUBLISHED, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(published));

        service.setPublication(courseId, assetId, Asset.PublicationStatus.PUBLISHED);

        verify(vectors).upsert(any());
    }

    @Test void formatsAssetVectorTimestampsAsIsoInsteadOfTheUiDateFormat() {
        assertThat(AssetVectorSyncService.isoTimestamp(LocalDateTime.of(2026, 9, 29, 14, 17, 25)))
                .isEqualTo("2026-09-29T14:17:25");
    }

    private ImageAsset image(Asset.PublicationStatus status, ImageAsset.PendingReplacement pending) {
        return image(status, pending, false);
    }

    private ImageAsset image(Asset.PublicationStatus status, ImageAsset.PendingReplacement pending, boolean aiGenerated) {
        Asset asset = new Asset(assetId, courseId, Asset.Type.IMAGE, "Title", "Description", null, List.of("tree"), status, Asset.UploadStatus.UPLOADED, LocalDateTime.now(), LocalDateTime.now());
        return new ImageAsset(asset, "source.png", "image/png", 12, "courses/source.png", pending, aiGenerated);
    }
}
