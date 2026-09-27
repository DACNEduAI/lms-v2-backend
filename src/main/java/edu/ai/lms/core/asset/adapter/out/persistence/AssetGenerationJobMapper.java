package edu.ai.lms.core.asset.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetGenerationJobMapper {
    int insert(AssetGenerationJobRow row);
    AssetGenerationJobRow find(@Param("courseId") UUID courseId, @Param("id") UUID id);
    AssetGenerationJobRow findForAsset(@Param("courseId") UUID courseId, @Param("assetId") UUID assetId);
    AssetGenerationJobRow findForUpdate(@Param("id") UUID id);
    List<AssetGenerationJobRow> findPending();
    int markProcessing(@Param("id") UUID id);
    int complete(AssetGenerationJobRow row);
    int fail(@Param("id") UUID id, @Param("message") String message, @Param("details") String details);
    int retry(@Param("id") UUID id);
    int restart(@Param("id") UUID id, @Param("prompt") String prompt, @Param("outputStorageKey") String outputStorageKey);
}
