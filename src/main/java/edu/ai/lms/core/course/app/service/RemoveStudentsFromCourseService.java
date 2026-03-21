package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.RemoveStudentsFromCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.StudentNotEnrolledInCourseException;
import edu.ai.lms.core.course.app.exception.StudentsNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.RemoveStudentsFromCourseCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherRemoveStudentsFromCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.LoadStudentsByEmailPort;
import edu.ai.lms.core.course.app.port.out.SaveCourseEnrollmentPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveStudentsFromCourseService implements TeacherRemoveStudentsFromCourseUseCase {

        private final SaveCourseEnrollmentPort saveCourseEnrollmentPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadStudentsByEmailPort loadStudentsByEmailPort;

    @Override
    @Transactional
    public RemoveStudentsFromCourseResponse removeStudentsFromCourse(RemoveStudentsFromCourseCommand command) {
        // Verify teacher is authenticated
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        // Verify teacher owns this course
        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to remove students from this course");
        }

        // Lookup students by email
        Map<String, UUID> emailToIdMap = loadStudentsByEmailPort.findStudentIdsByEmails(command.studentEmails());

        // Check for missing students (not in system)
        List<String> missingEmails = command.studentEmails().stream()
                .filter(email -> !emailToIdMap.containsKey(email))
                .toList();

        if (!missingEmails.isEmpty()) {
            throw new StudentsNotFoundException(missingEmails);
        }

        // Check that all students are actually enrolled in the course
        List<String> notEnrolledEmails = command.studentEmails().stream()
                .filter(email -> !course.getStudentIds().contains(emailToIdMap.get(email)))
                .toList();

        if (!notEnrolledEmails.isEmpty()) {
            throw new StudentNotEnrolledInCourseException(notEnrolledEmails);
        }

        // Remove the student IDs from the course
        List<UUID> idsToRemove = command.studentEmails().stream()
                .map(emailToIdMap::get)
                .toList();

        saveCourseEnrollmentPort.removeStudentsFromCourse(course.getId(), idsToRemove);

        return RemoveStudentsFromCourseResponse.builder()
                .courseId(course.getId())
                .removedStudentEmails(command.studentEmails())
                .totalRemoved(command.studentEmails().size())
                .message("Students removed from course successfully")
                .build();
    }
}
