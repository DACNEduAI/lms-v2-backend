package edu.ai.lms.core.quiz3.adapter.out.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.quiz3.app.port.out.LoadCourseMembershipPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizCourseMembershipAdapter implements LoadCourseMembershipPort {
    private final CourseAccessApi courseAccessApi;

    @Override
    public boolean isTeacherInCourse(UUID teacherId, UUID courseId) {
        return courseAccessApi.isTeacherInCourse(teacherId, courseId);
    }

    @Override
    public boolean isStudentInCourse(UUID studentId, UUID courseId) {
        return courseAccessApi.isStudentInCourse(studentId, courseId);
    }

    @Override
    public List<UUID> getStudentIdsByCourseId(UUID courseId) {
        return courseAccessApi.getStudentIds(courseId);
    }
}
