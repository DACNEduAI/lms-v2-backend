package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Operation(summary = "Get a specific course", description = "Get a course by its ID. The course must belong to the authenticated teacher.")
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

    @Operation(summary = "Get all courses", description = "Get all courses for the authenticated teacher with pagination and sorting")
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

    @Operation(summary = "Create a new course", description = "Create a new course")
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

    @Operation(summary = "Modify an existing course", description = "Update course details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyCourseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this course")
    })

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping()
    public ResponseEntity<ModifyCourseResponse> modifyCourse(@Valid @RequestBody ModifyCourseRequest request) {
        ModifyCourseCommand command = modifyCourseCommandMapper.toCommand(request);
        ModifyCourseResponse response = teacherModifyCourseUseCase.modifyCourse(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a course", description = "Delete an existing course")
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
