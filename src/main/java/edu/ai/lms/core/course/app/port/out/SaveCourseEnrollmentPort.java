package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.UUID;

public interface SaveCourseEnrollmentPort {

    void addStudentsToCourse(UUID courseId, List<UUID> studentIds);

    void removeStudentsFromCourse(UUID courseId, List<UUID> studentIds);
}
