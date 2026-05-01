package edu.ai.lms.core.notification.adapter.out.persistence;

import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface CourseAnnouncementRepository {
    void insert(CourseAnnouncement announcement);
    List<CourseAnnouncement> findByCourseId(@Param("courseId") UUID courseId, @Param("offset") long offset, @Param("limit") int limit);
    long countByCourseId(@Param("courseId") UUID courseId);
    Optional<CourseAnnouncement> findById(@Param("id") UUID id);
}
