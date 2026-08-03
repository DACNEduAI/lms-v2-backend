package edu.ai.lms.core.coderunner.adapter.out.integration;

import edu.ai.lms.core.coderunner.app.port.out.CheckCodeRunnerCourseAccessPort;
import edu.ai.lms.core.course.api.CourseAccessApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class CodeRunnerCourseAccessAdapter implements CheckCodeRunnerCourseAccessPort {

    private final CourseAccessApi courseAccessApi;

    @Override
    public boolean isTeacherInCourse(UUID teacherId, UUID courseId) {
        return courseAccessApi.isTeacherInCourse(teacherId, courseId);
    }
}
