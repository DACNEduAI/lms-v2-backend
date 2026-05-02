package edu.ai.lms.core.notification.app.port.in.usecase;

import edu.ai.lms.core.notification.adapter.in.http.request.CourseAnnouncementRequest;
import java.util.UUID;

public interface SendCourseAnnouncementUseCase {
    void sendAnnouncement(UUID courseId, CourseAnnouncementRequest request);
}
