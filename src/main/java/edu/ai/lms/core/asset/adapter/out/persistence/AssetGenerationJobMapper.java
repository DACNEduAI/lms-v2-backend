package edu.ai.lms.core.asset.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetGenerationJobMapper {
    int insert(AssetGenerationJobRow row);
    AssetGenerationJobRow find(@Param("courseId") UUID courseId, @Param("id") UUID id);
    AssetGenerationJobRow findForUpdate(@Param("id") UUID id);
    List<AssetGenerationJobRow> findPending();
    int markProcessing(@Param("id") UUID id);
    int complete(AssetGenerationJobRow row);
    int fail(@Param("id") UUID id, @Param("message") String message);
}
