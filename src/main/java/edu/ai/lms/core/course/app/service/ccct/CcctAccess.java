package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.exception.UnauthorizedTeacherException;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CcctAccess {
    private final LoadCoursePort coursePort;
    private final LoadSignedInUserPort signedInUserPort;

    CourseActor requireCourseTeacher(UUID courseId) {
        Course course = coursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));
        CurrentActor actor = signedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));
        if (actor.role() != Roles.TEACHER || !course.getTeacherIds().contains(actor.id())) {
            throw new UnauthorizedTeacherException("Teacher is not authorized to manage this course");
        }
        return new CourseActor(course, actor);
    }

    record CourseActor(Course course, CurrentActor actor) {
    }
}
