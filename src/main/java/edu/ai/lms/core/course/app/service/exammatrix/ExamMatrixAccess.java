package edu.ai.lms.core.course.app.service.exammatrix;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.exception.UnauthorizedTeacherException;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExamMatrixAccess {
    private final LoadCoursePort coursePort;
    private final LoadSignedInUserPort signedInUserPort;

    public Course requireCourseTeacher(java.util.UUID courseId) {
        Course course = coursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));
        var actor = signedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));
        if (!course.getTeacherIds().contains(actor.getId())) {
            throw new UnauthorizedTeacherException("Teacher is not authorized to manage this course");
        }
        return course;
    }
}
