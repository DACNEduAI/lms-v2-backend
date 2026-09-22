package edu.ai.lms.core.quiz3.app.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadCourseMembershipPort {

    /**
     * Checks whether a teacher is a member of the given course.
     */
    boolean isTeacherInCourse(UUID teacherId, UUID courseId);

    /**
     * Checks whether a student is a member of the given course.
     */
    boolean isStudentInCourse(UUID studentId, UUID courseId);

    /**
     * Returns all student IDs enrolled in the given course.
     * Used to broadcast notifications to all students when a quiz is created.
     */
    List<UUID> getStudentIdsByCourseId(UUID courseId);
}
