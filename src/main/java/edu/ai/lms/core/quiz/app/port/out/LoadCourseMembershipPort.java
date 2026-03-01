package edu.ai.lms.core.quiz.app.port.out;

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
}
