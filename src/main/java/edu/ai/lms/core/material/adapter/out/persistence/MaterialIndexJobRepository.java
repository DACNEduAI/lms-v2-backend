package edu.ai.lms.core.material.adapter.out.persistence;

import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface MaterialIndexJobRepository {
    void save(MaterialIndexJob job);
    Optional<MaterialIndexJob> findById(@Param("id") UUID id);
    int claim(@Param("id") UUID id, @Param("leaseUntil") LocalDateTime leaseUntil);
    List<UUID> findRecoverableIds(@Param("limit") int limit);
}
