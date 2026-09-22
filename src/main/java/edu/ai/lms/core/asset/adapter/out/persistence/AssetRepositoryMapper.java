package edu.ai.lms.core.asset.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface AssetRepositoryMapper {
    int insertAsset(AssetImageRow row);
    int insertImage(AssetImageRow row);
    List<AssetImageRow> findImages(@Param("courseId") UUID courseId);
    AssetImageRow findImage(@Param("courseId") UUID courseId, @Param("assetId") UUID assetId);
    int updateAssetLifecycle(AssetImageRow row);
    int updateImageUploadMetadata(AssetImageRow row);
    int delete(@Param("courseId") UUID courseId, @Param("assetId") UUID assetId);
}
