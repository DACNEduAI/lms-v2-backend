package edu.ai.lms.core.course.api;

import java.util.List;
import java.util.UUID;

public interface CourseAccessApi {

    boolean isTeacherInCourse(UUID teacherId, UUID courseId);

    boolean isStudentInCourse(UUID studentId, UUID courseId);

    List<UUID> getStudentIds(UUID courseId);
}
