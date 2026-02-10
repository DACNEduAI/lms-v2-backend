package edu.ai.lms.core.course.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.DeleteCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteCourse;
import edu.ai.lms.core.course.app.port.out.DeleteCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.exception.CourseNotFoundException;
import edu.ai.lms.core.course.exception.TeacherNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCourseService implements TeacherDeleteCourse {

    private final DeleteCoursePort deleteCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional
    public DeleteCourseResponse deleteCourse(UUID courseId) {
        // Validate course exists
        Course existingCourse = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));

        // Validate teacher exists and get authenticated teacher
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated teacher not found"));

        // Business rule: Validate that the teacher owns the course
        if (!existingCourse.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to delete this course");
        }

        // Delete the course
        deleteCoursePort.deleteById(courseId);

        // Return response
        return DeleteCourseResponse.builder()
                .courseId(courseId)
                .message("Course deleted successfully")
                .success(true)
                .build();
    }
}
