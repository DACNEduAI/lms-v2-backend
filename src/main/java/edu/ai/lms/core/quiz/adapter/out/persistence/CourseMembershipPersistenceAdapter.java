package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CourseMembershipPersistenceAdapter implements LoadCourseMembershipPort {

    private final CourseRepository courseRepository;

    @Override
    public boolean isTeacherInCourse(UUID teacherId, UUID courseId) {
        return courseRepository.existsByCourseIdAndTeacherId(courseId, teacherId);
    }

    @Override
    public boolean isStudentInCourse(UUID studentId, UUID courseId) {
        return courseRepository.existsByCourseIdAndStudentId(courseId, studentId);
    }
}
