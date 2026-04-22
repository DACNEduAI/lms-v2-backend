package edu.ai.lms.core.history.adapter.out.persistence;

import edu.ai.lms.core.history.app.domain.ActivityLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ActivityLogRepository {
    List<ActivityLog> findByUserId(@Param("userId") UUID userId, @Param("offset") int offset,
            @Param("limit") int limit);

    long countByUserId(@Param("userId") UUID userId);

    void insert(ActivityLog activityLog);
}