package edu.ai.lms.core.asset.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.exception.AssetValidationException;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import edu.ai.lms.core.material.api.StoredObjectMetadata;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageAssetServiceUT {
    @Mock private AssetRepository assets;
    @Mock private CourseTeacherAccess access;
    @Mock private CourseAssetStorageApi storage;
    private ImageAssetService service;
    private UUID courseId;
    private UUID assetId;

    @BeforeEach void setUp() {
        service = new ImageAssetService(assets, access, storage);
        courseId = UUID.randomUUID(); assetId = UUID.randomUUID();
        when(assets.saveImage(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void updatesMetadataWithoutChangingPublishedLifecycle() {
        ImageAsset original = image(Asset.PublicationStatus.PUBLISHED, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(original));

        ImageAsset updated = service.update(courseId, assetId,
                new ImageAssetService.UpdateImage("New title", "Updated description", "  prompt  ", "Reference material", List.of("Graph", "graph", " ")));

        assertThat(updated.asset().name()).isEqualTo("New title");
        assertThat(updated.asset().publicationStatus()).isEqualTo(Asset.PublicationStatus.PUBLISHED);
        assertThat(updated.asset().uploadStatus()).isEqualTo(Asset.UploadStatus.UPLOADED);
        assertThat(updated.asset().tags()).containsExactly("graph");
        assertThat(updated.storageKey()).isEqualTo("courses/source.png");
    }

    @Test void promotesOnlyVerifiedReplacementAndKeepsPublishedStatus() {
        ImageAsset original = image(Asset.PublicationStatus.PUBLISHED, null);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(original));
        when(storage.createUploadLink(any(), eq("image/jpeg"))).thenReturn("https://upload");

        ImageAssetService.ReplacementUpload upload = service.beginReplacement(courseId, assetId,
                new ImageAssetService.ReplacementImage("replacement.jpg", "image/jpeg"));
        ArgumentCaptor<ImageAsset> staged = ArgumentCaptor.forClass(ImageAsset.class);
        verify(assets).saveImage(staged.capture());
        ImageAsset pending = staged.getValue();
        assertThat(upload.presignedUploadUrl()).isEqualTo("https://upload");
        assertThat(pending.pendingReplacement().storageKey()).contains("/replacements/");

        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(pending));
        when(storage.inspect(pending.pendingReplacement().storageKey())).thenReturn(new StoredObjectMetadata(true, 42, "image/jpeg"));

        ImageAsset promoted = service.confirmReplacement(courseId, assetId);

        assertThat(promoted.originalFileName()).isEqualTo("replacement.jpg");
        assertThat(promoted.byteSize()).isEqualTo(42);
        assertThat(promoted.pendingReplacement()).isNull();
        assertThat(promoted.asset().publicationStatus()).isEqualTo(Asset.PublicationStatus.PUBLISHED);
        verify(storage).delete("courses/source.png");
    }

    @Test void rejectsInvalidReplacementWithoutChangingCurrentImage() {
        ImageAsset.PendingReplacement pending = new ImageAsset.PendingReplacement("replacement.png", "image/png", "courses/replacement.png");
        ImageAsset original = image(Asset.PublicationStatus.DRAFT, pending);
        when(assets.findImage(courseId, assetId)).thenReturn(Optional.of(original));
        when(storage.inspect(pending.storageKey())).thenReturn(StoredObjectMetadata.missing());

        assertThatThrownBy(() -> service.confirmReplacement(courseId, assetId)).isInstanceOf(AssetValidationException.class);

        ArgumentCaptor<ImageAsset> saved = ArgumentCaptor.forClass(ImageAsset.class);
        verify(assets).saveImage(saved.capture());
        assertThat(saved.getValue().storageKey()).isEqualTo("courses/source.png");
        assertThat(saved.getValue().pendingReplacement()).isNull();
        verify(storage).delete(pending.storageKey());
    }

    private ImageAsset image(Asset.PublicationStatus status, ImageAsset.PendingReplacement pending) {
        Asset asset = new Asset(assetId, courseId, Asset.Type.IMAGE, "Title", "Description", null, "Lecture material", List.of("tree"), status, Asset.UploadStatus.UPLOADED, LocalDateTime.now(), LocalDateTime.now());
        return new ImageAsset(asset, "source.png", "image/png", 12, "courses/source.png", pending);
    }
}
