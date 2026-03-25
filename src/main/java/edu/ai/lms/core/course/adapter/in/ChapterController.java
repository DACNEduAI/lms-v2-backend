package edu.ai.lms.core.course.adapter.in;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.mapper.CreateChapterCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.ModifyChapterCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.CreateChapterRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyChapterRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.adapter.in.response.DeleteChapterResponse;
import edu.ai.lms.core.course.adapter.in.response.GetChapterResponse;
import edu.ai.lms.core.course.adapter.in.response.ModifyChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifyChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllChaptersUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.GetChapterUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateChapterUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteChapterUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyChapterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final TeacherCreateChapterUseCase teacherCreateChapterUseCase;
    private final TeacherModifyChapterUseCase teacherModifyChapterUseCase;
    private final TeacherDeleteChapterUseCase teacherDeleteChapterUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final GetAllChaptersUseCase getAllChaptersUseCase;
    private final CreateChapterCommandMapper createChapterCommandMapper;
    private final ModifyChapterCommandMapper modifyChapterCommandMapper;

    @Operation(
            summary = "Get all chapters for a course",
            description = """
                    Returns all chapters for a course.

                    LMS business logic:
                    - Exposes course structure used by learners/teachers for navigation.

                    Path fields:
                    - courseId: Parent course ID.

                    Response fields per chapter:
                    - chapterId, title, description, courseId.
                    - learningOutcomeIds.
                    - ungroupedLearningOutcomes[] and groupedLearningOutcomes[].
                    - createdAt, updatedAt.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapters retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this course")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping
    public ResponseEntity<List<GetChapterResponse>> getAllChapters(
            @PathVariable UUID courseId) {
        List<GetChapterResponse> response = getAllChaptersUseCase.getAllChapters(courseId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get a specific chapter",
            description = """
                    Returns one chapter in a course.

                    LMS business logic:
                    - Retrieves chapter content scope and mapped learning outcomes.

                    Path fields:
                    - courseId: Course ID.
                    - chapterId: Chapter ID.

                    Response fields:
                    - chapterId, title, description, courseId.
                    - learningOutcomeIds.
                    - ungroupedLearningOutcomes[] and groupedLearningOutcomes[].
                    - createdAt, updatedAt.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Chapter not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this chapter")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{chapterId}")
    public ResponseEntity<GetChapterResponse> getChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId) {
        GetChapterResponse response = getChapterUseCase.getChapter(courseId, chapterId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create a new chapter",
            description = """
                    Creates a chapter in a course.

                    LMS business logic:
                    - Course owner teacher adds a new instructional unit to course structure.

                    Path fields:
                    - courseId: Course ID.

                    Request fields:
                    - title: Chapter title.
                    - description: Chapter description.

                    Response fields:
                    - chapterId, title, description, courseId, createdAt.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter created successfully",
                    content = @Content(schema = @Schema(implementation = CreateChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add chapters to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public ResponseEntity<CreateChapterResponse> createChapter(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateChapterRequest request) {
        CreateChapterCommand command = createChapterCommandMapper.toCommand(courseId, request);
        CreateChapterResponse response = teacherCreateChapterUseCase.createChapter(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Modify an existing chapter",
            description = """
                    Updates chapter metadata.

                    LMS business logic:
                    - Course owner teacher edits chapter structure details.

                    Path fields:
                    - courseId: Course ID.
                    - chapterId: Chapter ID.

                    Request fields:
                    - title: Updated chapter title.
                    - description: Updated chapter description.

                    Response fields:
                    - chapterId, title, description, courseId, updatedAt.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or chapter not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify chapters in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{chapterId}")
    public ResponseEntity<ModifyChapterResponse> modifyChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody ModifyChapterRequest request) {
        ModifyChapterCommand command = modifyChapterCommandMapper.toCommand(courseId, chapterId, request);
        ModifyChapterResponse response = teacherModifyChapterUseCase.modifyChapter(command);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a chapter",
            description = """
                    Deletes a chapter from a course.

                    LMS business logic:
                    - Course owner teacher removes an instructional unit from the course structure.

                    Path fields:
                    - courseId: Course ID.
                    - chapterId: Chapter ID.

                    Response fields:
                    - courseId, chapterId, message, success.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or chapter not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete chapters in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{chapterId}")
    public ResponseEntity<DeleteChapterResponse> deleteChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId) {
        DeleteChapterResponse response = teacherDeleteChapterUseCase.deleteChapter(courseId, chapterId);
        return ResponseEntity.ok(response);
    }
}
