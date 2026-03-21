package edu.ai.lms.core.course.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.DeleteCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteCourseUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.material.adapter.in.internal.DeleteCourseMaterialsApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCourseService implements TeacherDeleteCourseUseCase {

    private final DeleteCoursePort deleteCoursePort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final DeleteCourseMaterialsApi deleteCourseMaterialsApi;

    @Override
    @Transactional
    public DeleteCourseResponse deleteCourse(UUID courseId) {
        // Validate course exists
        Course existingCourse = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));

        // Validate teacher exists and get authenticated teacher
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Authenticated teacher not found"));

        // Business rule: Validate that the teacher is associated with the course
        if (!existingCourse.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to delete this course");
        }

        // Delete S3 files for all materials belonging to this course
        // (must happen before DB cascade deletes material records)
        deleteCourseMaterialsApi.deleteAllMaterialsByCourseId(courseId);

        // Delete the course (DB cascades handle chapters, learning outcomes, material records)
        deleteCoursePort.deleteById(courseId);

        // Return response
        return DeleteCourseResponse.builder()
                .courseId(courseId)
                .message("Course deleted successfully")
                .success(true)
                .build();
    }
}
