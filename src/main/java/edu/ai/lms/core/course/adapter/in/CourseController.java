package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.mapper.CreateCourseCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.ModifyCourseCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.adapter.in.response.DeleteCourseResponse;
import edu.ai.lms.core.course.adapter.in.response.GetAllCoursesResponse;
import edu.ai.lms.core.course.adapter.in.response.GetCourseResponse;
import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateCourseCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifyCourseCommand;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllCoursesUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.GetCourseUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateCourseUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteCourseUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyCourseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final TeacherCreateCourseUseCase teacherCreateCourseUseCase;
    private final TeacherModifyCourseUseCase teacherModifyCourseUseCase;
    private final TeacherDeleteCourseUseCase teacherDeleteCourseUseCase;
    private final GetCourseUseCase getCourseUseCase;
    private final GetAllCoursesUseCase getAllCoursesUseCase;
    private final CreateCourseCommandMapper createCourseCommandMapper;
    private final ModifyCourseCommandMapper modifyCourseCommandMapper;

    @Operation(
            summary = "Get a specific course",
            description = """
                    Returns one course by ID.

                    LMS business logic:
                    - Loads the course aggregate and its learning outcome structure.
                    - Accessible to authorized teachers/students in the course context.

                    Path fields:
                    - courseId: Target course ID.

                    Response fields:
                    - courseId, courseName, courseCode, description, teacherId, active, coverImage, startDate, endDate.
                    - groupLearningOutcomes[]: Grouped CLOs (id, name, description, learningOutcomes[]).
                    - ungroupedLearningOutcomes[]: CLOs not assigned to groups (id, code, description).
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetCourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this course")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{courseId}")
    public ResponseEntity<GetCourseResponse> getCourse(@PathVariable UUID courseId) {
        GetCourseResponse response = getCourseUseCase.getCourse(courseId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all courses",
            description = """
                    Returns a course list with pagination/sorting options.

                    LMS business logic:
                    - Provides course discovery/list view for authorized users.

                    Query fields:
                    - pageSize: Number of courses to return.
                    - sortDirection: Sort direction (asc/desc).

                    Response fields:
                    - courses[]: Course summary items (courseId, courseName, courseCode, description, active, coverImage, startDate, endDate).
                    - totalCourses: Count returned in this response context.
                    - pageSize: Applied page size.
                    - sortDirection: Applied sort direction.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetAllCoursesResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping()
    public ResponseEntity<GetAllCoursesResponse> getAllCourses(
            @Parameter(description = "Number of courses to return", example = "5")
            @RequestParam(defaultValue = "5") int pageSize,
            @Parameter(description = "Sort direction by course name (asc or desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDirection) {
        GetAllCoursesResponse response = getAllCoursesUseCase.getAllCourses(pageSize, sortDirection);
        return ResponseEntity.ok(response);
    }

                @Operation(
                                                summary = "Create a new course",
                                                description = """
                                                                                Creates a course for the authenticated teacher.

                                                                                LMS business logic:
                                                                                - Teacher becomes course owner.
                                                                                - Validates course metadata and date range.
                                                                                - Can seed initial learning outcomes at creation time.

                                                                                Request fields:
                                                                                - courseName: Course title.
                                                                                - description: Course description.
                                                                                - courseCode: Institutional course code.
                                                                                - active: Course visibility/availability state.
                                                                                - coverImage: Cover image reference.
                                                                                - startDate: Course start date.
                                                                                - endDate: Course end date (must be after startDate).
                                                                                - learningOutcomes[]: Optional initial CLO list.
                                                                                        - code: CLO code.
                                                                                        - description: CLO description.

                                                                                Response fields:
                                                                                - courseId, courseName, courseCode, description, teacherId, active, coverImage, startDate, endDate.
                                                                                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully",
                    content = @Content(schema = @Schema(implementation = CreateCourseResponse.class))),
    })

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public ResponseEntity<CreateCourseResponse> registerCourse(@Valid @RequestBody CreateCourseRequest request) {
        CreateCourseCommand command = createCourseCommandMapper.toCommand(request);
        CreateCourseResponse response = teacherCreateCourseUseCase.createCourse(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Modify an existing course",
            description = """
                    Updates an existing course.

                    LMS business logic:
                    - Only the course owner teacher can modify course metadata.
                    - Date-range validity is enforced.

                    Path fields:
                    - courseId: Target course ID.

                    Request fields:
                    - courseName, description, courseCode, active, coverImage, startDate, endDate.

                    Response fields:
                    - courseId, courseName, courseCode, description, teacherId, active, coverImage, startDate, endDate, updatedAt.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyCourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this course")
    })

    @PreAuthorize("hasRole('TEACHER')")
        @PutMapping("/{courseId}")
        public ResponseEntity<ModifyCourseResponse> modifyCourse(
                        @PathVariable UUID courseId,
                        @Valid @RequestBody ModifyCourseRequest request) {
                ModifyCourseCommand command = modifyCourseCommandMapper.toCommand(courseId, request);
        ModifyCourseResponse response = teacherModifyCourseUseCase.modifyCourse(command);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a course",
            description = """
                    Deletes a course.

                    LMS business logic:
                    - Only the owning teacher can remove the course.
                    - Removal applies to the course aggregate and related associations.

                    Path fields:
                    - courseId: Target course ID.

                    Response fields:
                    - courseId: Deleted course ID.
                    - message: Operation result message.
                    - success: Deletion status flag.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteCourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this course")
    })

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<DeleteCourseResponse> deleteCourse(@PathVariable UUID courseId) {
        DeleteCourseResponse response = teacherDeleteCourseUseCase.deleteCourse(courseId);
        return ResponseEntity.ok(response);
    }

}
