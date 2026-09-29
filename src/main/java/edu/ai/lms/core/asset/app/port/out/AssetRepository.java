package edu.ai.lms.core.asset.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;

public interface AssetRepository {
    ImageAsset createImage(ImageAsset image);
    List<ImageAsset> findImages(UUID courseId);
    List<ImageAsset> findPublishedUploadedImages();
    Optional<ImageAsset> findImage(UUID courseId, UUID assetId);
    ImageAsset saveImage(ImageAsset image);
    boolean delete(UUID courseId, UUID assetId);
}
