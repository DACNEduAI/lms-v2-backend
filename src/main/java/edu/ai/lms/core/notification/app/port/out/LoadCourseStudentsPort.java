package edu.ai.lms.core.notification.app.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadCourseStudentsPort {
    List<UUID> findStudentIdsByCourseId(UUID courseId);

    boolean isTeacherInCourse(UUID teacherId, UUID courseId);
}
