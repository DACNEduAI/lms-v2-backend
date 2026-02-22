package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.mapper.CreateChapterCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.CreateChapterRequest;
import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.adapter.in.response.GetChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.GetChapterUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateChapterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final TeacherCreateChapterUseCase teacherCreateChapterUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final CreateChapterCommandMapper createChapterCommandMapper;

    @Operation(summary = "Get a specific chapter", description = "Get a chapter by its ID. The chapter's course must belong to the authenticated teacher.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Chapter not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this chapter")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{chapterId}")
    public ResponseEntity<GetChapterResponse> getChapter(@PathVariable UUID chapterId) {
        GetChapterResponse response = getChapterUseCase.getChapter(chapterId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new chapter", description = "Create a new chapter within a course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter created successfully",
                    content = @Content(schema = @Schema(implementation = CreateChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add chapters to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public ResponseEntity<CreateChapterResponse> createChapter(@Valid @RequestBody CreateChapterRequest request) {
        CreateChapterCommand command = createChapterCommandMapper.toCommand(request);
        CreateChapterResponse response = teacherCreateChapterUseCase.createChapter(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
