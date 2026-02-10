package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.request.CreateCourseRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyCourseRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateCourseResponse;
import edu.ai.lms.core.course.adapter.in.response.DeleteCourseResponse;
import edu.ai.lms.core.course.adapter.in.response.ModifyCourseResponse;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateCourse;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteCourse;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyCourse;
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
public class CourseController {

    @Autowired
    TeacherCreateCourse teacherCreateCourse;
    TeacherModifyCourse teacherModifyCourse;
    TeacherDeleteCourse teacherDeleteCourse;

    @Operation(summary = "Create a new course", description = "Create a new course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully",
                    content = @Content(schema = @Schema(implementation = CreateCourseResponse.class))),
    })

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public ResponseEntity<CreateCourseResponse> registerCourse(@Valid @RequestBody CreateCourseRequest request) {
        CreateCourseResponse response = teacherCreateCourse.createCourse(request);
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
        ModifyCourseResponse response = teacherModifyCourse.modifyCourse(request);
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
        DeleteCourseResponse response = teacherDeleteCourse.deleteCourse(courseId);
        return ResponseEntity.ok(response);
    }

}
