package edu.ai.lms.core.notification.app.port.out;

import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface CourseAnnouncementPort {
    CourseAnnouncement save(CourseAnnouncement announcement);
    Page<CourseAnnouncement> findByCourseId(UUID courseId, Pageable pageable);
    Optional<CourseAnnouncement> findById(UUID id);
}
