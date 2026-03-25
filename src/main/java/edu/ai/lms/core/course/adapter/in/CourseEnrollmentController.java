package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.mapper.AddStudentsToCourseCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.RemoveStudentsFromCourseCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.AddStudentsToCourseRequest;
import edu.ai.lms.core.course.adapter.in.request.RemoveStudentsFromCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.AddStudentsToCourseResponse;
import edu.ai.lms.core.course.adapter.in.response.GetCourseEnrollmentsResponse;
import edu.ai.lms.core.course.adapter.in.response.RemoveStudentsFromCourseResponse;
import edu.ai.lms.core.course.app.port.in.command.AddStudentsToCourseCommand;
import edu.ai.lms.core.course.app.port.in.command.RemoveStudentsFromCourseCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherAddStudentsToCourseUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherGetCourseEnrollmentsUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherRemoveStudentsFromCourseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseEnrollmentController {

    private final TeacherAddStudentsToCourseUseCase teacherAddStudentsToCourseUseCase;
    private final TeacherRemoveStudentsFromCourseUseCase teacherRemoveStudentsFromCourseUseCase;
    private final TeacherGetCourseEnrollmentsUseCase teacherGetCourseEnrollmentsUseCase;
    private final AddStudentsToCourseCommandMapper addStudentsToCourseCommandMapper;
    private final RemoveStudentsFromCourseCommandMapper removeStudentsFromCourseCommandMapper;

    @Operation(
            summary = "Get course enrollments",
            description = """
                    Returns course enrollment participants.

                    LMS business logic:
                    - Course owner teacher retrieves participant rosters for management.

                    Path fields:
                    - courseId: Course ID.

                    Response fields:
                    - courseId: Target course ID.
                    - teachers[]: Teacher user IDs in the course.
                    - students[]: Student user IDs in the course.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course enrollments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetCourseEnrollmentsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to view course enrollments")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{courseId}/enrollments")
    public ResponseEntity<GetCourseEnrollmentsResponse> getCourseEnrollments(@PathVariable UUID courseId) {
        GetCourseEnrollmentsResponse response = teacherGetCourseEnrollmentsUseCase.getCourseEnrollments(courseId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Add students to a course",
            description = """
                    Adds students to a course by email.

                    LMS business logic:
                    - Course owner teacher enrolls existing student accounts.

                    Path fields:
                    - courseId: Course ID.

                    Request fields:
                    - studentEmails[]: Student email addresses to enroll.

                    Response fields:
                    - courseId: Course ID.
                    - addedStudentEmails[]: Emails successfully enrolled.
                    - totalAdded: Number of added students.
                    - message: Operation message.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Students added to course successfully",
                    content = @Content(schema = @Schema(implementation = AddStudentsToCourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found or students not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add students to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{courseId}/students/add")
    public ResponseEntity<AddStudentsToCourseResponse> addStudentsToCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody AddStudentsToCourseRequest request) {
        AddStudentsToCourseCommand command = addStudentsToCourseCommandMapper.toCommand(courseId, request);
        AddStudentsToCourseResponse response = teacherAddStudentsToCourseUseCase.addStudentsToCourse(command);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove students from a course",
            description = """
                    Removes students from a course by email.

                    LMS business logic:
                    - Course owner teacher unenrolls students from the course roster.

                    Path fields:
                    - courseId: Course ID.

                    Request fields:
                    - studentEmails[]: Student email addresses to remove.

                    Response fields:
                    - courseId: Course ID.
                    - removedStudentEmails[]: Emails successfully removed.
                    - totalRemoved: Number of removed students.
                    - message: Operation message.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Students removed from course successfully",
                    content = @Content(schema = @Schema(implementation = RemoveStudentsFromCourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found or students not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to remove students from this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{courseId}/students/remove")
    public ResponseEntity<RemoveStudentsFromCourseResponse> removeStudentsFromCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody RemoveStudentsFromCourseRequest request) {
        RemoveStudentsFromCourseCommand command = removeStudentsFromCourseCommandMapper.toCommand(courseId, request);
        RemoveStudentsFromCourseResponse response = teacherRemoveStudentsFromCourseUseCase.removeStudentsFromCourse(command);
        return ResponseEntity.ok(response);
    }

}
