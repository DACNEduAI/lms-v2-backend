package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

public interface CleanupCourseMaterialsPort {
    void deleteAllByCourseId(UUID courseId);
}
