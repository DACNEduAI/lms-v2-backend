package edu.ai.lms.core.coderunner.app.port.out;

import java.util.UUID;

public interface CheckCodeRunnerCourseAccessPort {

    boolean isTeacherInCourse(UUID teacherId, UUID courseId);
}
