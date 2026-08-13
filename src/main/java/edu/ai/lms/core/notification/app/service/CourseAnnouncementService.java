package edu.ai.lms.core.notification.app.service;

import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.notification.adapter.in.http.request.CourseAnnouncementRequest;
import edu.ai.lms.core.notification.app.domain.CourseAnnouncement;
import edu.ai.lms.core.notification.app.port.in.usecase.GetCourseAnnouncementsUseCase;
import edu.ai.lms.core.notification.app.port.in.usecase.SendCourseAnnouncementUseCase;
import edu.ai.lms.core.notification.app.port.out.CourseAnnouncementPort;
import edu.ai.lms.core.notification.app.port.out.LoadCourseStudentsPort;
import edu.ai.lms.core.notification.app.port.out.LoadCurrentUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseAnnouncementService implements SendCourseAnnouncementUseCase, GetCourseAnnouncementsUseCase {

    private final LoadCourseStudentsPort loadCourseStudentsPort;
    private final CourseAnnouncementPort courseAnnouncementPort;
    private final LoadCurrentUserPort loadCurrentUserPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void sendAnnouncement(UUID courseId, CourseAnnouncementRequest request) {
        // 1. Lưu thông báo gốc vào bảng course_announcements
        UUID teacherId = loadCurrentUserPort.getCurrentUserId()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated"));
        if (!loadCourseStudentsPort.isTeacherInCourse(teacherId, courseId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Teacher is not a member of this course");
        }
        CourseAnnouncement announcement = CourseAnnouncement.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .content(request.getMessage())
                .teacherId(teacherId)
                .type(request.getType() != null ? request.getType() : "INFO")
                .createdAt(LocalDateTime.now(java.time.Clock.systemUTC()))
                .build();
        courseAnnouncementPort.save(announcement);

        // 2. Lấy danh sách ID sinh viên thuộc khóa học
        List<UUID> studentUserIds = loadCourseStudentsPort.findStudentIdsByCourseId(courseId);

        if (studentUserIds.isEmpty()) {
            return;
        }

        // 2. Đóng gói Event với đúng type từ Database (INFO, WARNING, URGENT)
        SystemNotificationEvent event = new SystemNotificationEvent(
                new HashSet<>(studentUserIds),
                request.getTitle(),
                request.getMessage(),
                announcement.getType(),
                "/student/courses/" + courseId
        );

        // 4. Bắn sự kiện ra để Listener tự động lưu DB và đẩy Real-time
        eventPublisher.publishEvent(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseAnnouncement> getAnnouncements(UUID courseId, Pageable pageable) {
        return courseAnnouncementPort.findByCourseId(courseId, pageable);
    }
}
