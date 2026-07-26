package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.AddStudentsToCourseResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.StudentsNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.AddStudentsToCourseCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherAddStudentsToCourseUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.LoadStudentsByEmailPort;
import edu.ai.lms.core.course.app.port.out.SaveCourseEnrollmentPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddStudentsToCourseService implements TeacherAddStudentsToCourseUseCase {

    private final SaveCourseEnrollmentPort saveCourseEnrollmentPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadStudentsByEmailPort loadStudentsByEmailPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AddStudentsToCourseResponse addStudentsToCourse(AddStudentsToCourseCommand command) {
        // Verify teacher is authenticated
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        // Verify teacher owns this course
        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to add students to this course");
        }

        // Lookup students by email
        Map<String, UUID> emailToIdMap = loadStudentsByEmailPort.findStudentIdsByEmails(command.studentEmails());

        // Check for missing students
        List<String> missingEmails = command.studentEmails().stream()
                .filter(email -> !emailToIdMap.containsKey(email))
                .toList();

        if (!missingEmails.isEmpty()) {
            throw new StudentsNotFoundException(missingEmails);
        }

        // Add student IDs to the course (avoid duplicates)
        List<UUID> currentStudentIds = new ArrayList<>(course.getStudentIds());
        List<String> actuallyAddedEmails = new ArrayList<>();

        for (String email : command.studentEmails()) {
            UUID studentId = emailToIdMap.get(email);
            if (!currentStudentIds.contains(studentId)) {
                currentStudentIds.add(studentId);
                actuallyAddedEmails.add(email);
            }
        }

        List<UUID> idsToAdd = actuallyAddedEmails.stream()
            .map(emailToIdMap::get)
            .toList();

        if (!idsToAdd.isEmpty()) {
            saveCourseEnrollmentPort.addStudentsToCourse(course.getId(), idsToAdd);
        }

        AddStudentsToCourseResponse response = AddStudentsToCourseResponse.builder()
                .courseId(course.getId())
                .addedStudentEmails(actuallyAddedEmails)
                .totalAdded(actuallyAddedEmails.size())
                .message("Students added to course successfully")
                .build();

        // Broadcast notification to the newly added students
        if (!idsToAdd.isEmpty()) {
            Set<UUID> studentIds = new HashSet<>(idsToAdd);
            eventPublisher.publishEvent(new SystemNotificationEvent(
                    studentIds,
                    "Chào mừng khóa học mới!",
                    "Bạn vừa được ghi danh vào khóa học: " + course.getCourseName(),
                    "COURSE_UPDATE",
                    "/student/courses/" + course.getId()
            ));
        }

        return response;
    }
}
