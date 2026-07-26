package edu.ai.lms.core.notification.adapter.out.integration;

import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.notification.app.port.out.LoadCourseStudentsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseNotificationAdapter implements LoadCourseStudentsPort {

    private final CourseAccessApi courseAccessApi;

    @Override
    public List<UUID> findStudentIdsByCourseId(UUID courseId) {
        return courseAccessApi.getStudentIds(courseId);
    }

    @Override
    public boolean isTeacherInCourse(UUID teacherId, UUID courseId) {
        return courseAccessApi.isTeacherInCourse(teacherId, courseId);
    }
}
