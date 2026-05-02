package edu.ai.lms.core.notification.app.port.in.usecase;

import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface GetCourseAnnouncementsUseCase {
    Page<CourseAnnouncement> getAnnouncements(UUID courseId, Pageable pageable);
}
