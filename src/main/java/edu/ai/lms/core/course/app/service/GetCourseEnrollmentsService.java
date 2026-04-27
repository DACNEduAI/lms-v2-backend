package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.GetCourseEnrollmentsResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherGetCourseEnrollmentsUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCourseEnrollmentsService implements TeacherGetCourseEnrollmentsUseCase {

    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional(readOnly = true)
    public GetCourseEnrollmentsResponse getCourseEnrollments(UUID courseId) {
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        Course course = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));

        boolean isTeacherOfCourse = course.getTeacherIds().contains(user.getId());
        boolean isStudentInCourse = course.getStudentIds().contains(user.getId());

        if (!isTeacherOfCourse && !isStudentInCourse) {
            throw new IllegalArgumentException("User is not authorized to access enrollments of this course");
        }

        return GetCourseEnrollmentsResponse.builder()
                .courseId(courseId)
                .teachers(course.getTeacherIds())
                .students(course.getStudentIds())
                .build();
    }
}
