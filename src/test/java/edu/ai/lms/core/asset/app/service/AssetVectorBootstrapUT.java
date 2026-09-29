package edu.ai.lms.core.asset.app.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetVectorBootstrapUT {
    @Mock private AssetRepository assets;
    @Mock private AssetVectorSyncService vectors;

    @Test void indexesOnlyPublishedUploadedImagesOnceAfterSuccess() {
        ImageAsset image = image();
        when(assets.findPublishedUploadedImages()).thenReturn(List.of(image));
        AssetVectorBootstrap bootstrap = new AssetVectorBootstrap(assets, vectors);

        bootstrap.syncUntilComplete();
        bootstrap.syncUntilComplete();

        verify(vectors).upsert(image);
        verify(assets).findPublishedUploadedImages();
    }

    @Test void retriesTheFullScanAfterAnIndexingFailure() {
        ImageAsset image = image();
        when(assets.findPublishedUploadedImages()).thenReturn(List.of(image));
        doThrow(new RuntimeException("AI unavailable")).doNothing().when(vectors).upsert(any());
        AssetVectorBootstrap bootstrap = new AssetVectorBootstrap(assets, vectors);

        bootstrap.syncUntilComplete();
        bootstrap.syncUntilComplete();

        verify(vectors, times(2)).upsert(image);
        verify(assets, times(2)).findPublishedUploadedImages();
    }

    private static ImageAsset image() {
        UUID courseId = UUID.randomUUID(); UUID assetId = UUID.randomUUID();
        Asset asset = new Asset(assetId, courseId, Asset.Type.IMAGE, "Tree", "Binary tree", null, List.of(),
                Asset.PublicationStatus.PUBLISHED, Asset.UploadStatus.UPLOADED, LocalDateTime.now(), LocalDateTime.now());
        return new ImageAsset(asset, "tree.png", "image/png", 1, "courses/tree.png", null, false);
    }
}
