package edu.ai.lms.core.course.adapter.in;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.course.adapter.in.mapper.AddLearningOutcomesToChapterCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.CreateLearningOutcomesCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.DeleteLearningOutcomesCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.ModifyLearningOutcomesCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.ModifySingleLearningOutcomeCommandMapper;
import edu.ai.lms.core.course.adapter.in.mapper.RemoveLearningOutcomesFromChapterCommandMapper;
import edu.ai.lms.core.course.adapter.in.request.AddLearningOutcomesToChapterRequest;
import edu.ai.lms.core.course.adapter.in.request.CreateLearningOutcomesRequest;
import edu.ai.lms.core.course.adapter.in.request.DeleteLearningOutcomesRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyLearningOutcomeRequest;
import edu.ai.lms.core.course.adapter.in.request.ModifyLearningOutcomesRequest;
import edu.ai.lms.core.course.adapter.in.request.RemoveLearningOutcomesFromChapterRequest;
import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToChapterResponse;
import edu.ai.lms.core.course.adapter.in.response.CreateLearningOutcomesResponse;
import edu.ai.lms.core.course.adapter.in.response.DeleteLearningOutcomesResponse;
import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomeResponse;
import edu.ai.lms.core.course.adapter.in.response.ModifyLearningOutcomesResponse;
import edu.ai.lms.core.course.adapter.in.response.RemoveLearningOutcomesFromChapterResponse;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToChapterCommand;
import edu.ai.lms.core.course.app.port.in.command.CreateLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.command.DeleteLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifyLearningOutcomesCommand;
import edu.ai.lms.core.course.app.port.in.command.ModifySingleLearningOutcomeCommand;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherAddLearningOutcomesToChapterUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyLearningOutcomesUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifySingleLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherRemoveLearningOutcomesFromChapterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LearningOutcomeController {

    private final TeacherCreateLearningOutcomesUseCase teacherCreateLearningOutcomesUseCase;
    private final TeacherModifySingleLearningOutcomeUseCase teacherModifySingleLearningOutcomeUseCase;
    private final TeacherModifyLearningOutcomesUseCase teacherModifyLearningOutcomesUseCase;
    private final TeacherDeleteLearningOutcomesUseCase teacherDeleteLearningOutcomesUseCase;
    private final TeacherAddLearningOutcomesToChapterUseCase teacherAddLearningOutcomesToChapterUseCase;
    private final TeacherRemoveLearningOutcomesFromChapterUseCase teacherRemoveLearningOutcomesFromChapterUseCase;
    private final CreateLearningOutcomesCommandMapper createLearningOutcomesCommandMapper;
    private final ModifySingleLearningOutcomeCommandMapper modifySingleLearningOutcomeCommandMapper;
    private final ModifyLearningOutcomesCommandMapper modifyLearningOutcomesCommandMapper;
    private final DeleteLearningOutcomesCommandMapper deleteLearningOutcomesCommandMapper;
    private final AddLearningOutcomesToChapterCommandMapper addLearningOutcomesToChapterCommandMapper;
    private final RemoveLearningOutcomesFromChapterCommandMapper removeLearningOutcomesFromChapterCommandMapper;

    // ==================== COURSE-LEVEL LEARNING OUTCOME MANAGEMENT ====================

    @Operation(summary = "Create learning outcomes", description = "Create one or more learning outcomes for a course. Only the course owner (teacher) can create learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Learning outcomes created successfully",
                    content = @Content(schema = @Schema(implementation = CreateLearningOutcomesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to create learning outcomes for this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/{courseId}/learning-outcome")
    public ResponseEntity<CreateLearningOutcomesResponse> createLearningOutcomes(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateLearningOutcomesRequest request) {
        CreateLearningOutcomesCommand command = createLearningOutcomesCommandMapper.toCommand(courseId, request);
        CreateLearningOutcomesResponse response = teacherCreateLearningOutcomesUseCase.createLearningOutcomes(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Modify a single learning outcome", description = "Modify a specific learning outcome by its ID. Only the course owner (teacher) can modify learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcome modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyLearningOutcomeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or learning outcome not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify learning outcomes in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/learning-outcome/{learningOutcomeId}")
    public ResponseEntity<ModifyLearningOutcomeResponse> modifyLearningOutcome(
            @PathVariable UUID courseId,
            @PathVariable UUID learningOutcomeId,
            @Valid @RequestBody ModifyLearningOutcomeRequest request) {
        ModifySingleLearningOutcomeCommand command = modifySingleLearningOutcomeCommandMapper.toCommand(
                courseId, learningOutcomeId, request);
        ModifyLearningOutcomeResponse response = teacherModifySingleLearningOutcomeUseCase.modifyLearningOutcome(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Modify multiple learning outcomes", description = "Modify one or more learning outcomes in batch. Only the course owner (teacher) can modify learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyLearningOutcomesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or learning outcome not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify learning outcomes in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/learning-outcome")
    public ResponseEntity<ModifyLearningOutcomesResponse> modifyLearningOutcomes(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModifyLearningOutcomesRequest request) {
        ModifyLearningOutcomesCommand command = modifyLearningOutcomesCommandMapper.toCommand(courseId, request);
        ModifyLearningOutcomesResponse response = teacherModifyLearningOutcomesUseCase.modifyLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete multiple learning outcomes", description = "Delete one or more learning outcomes by their IDs. Only the course owner (teacher) can delete learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteLearningOutcomesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or learning outcome not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete learning outcomes in this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/{courseId}/learning-outcome")
    public ResponseEntity<DeleteLearningOutcomesResponse> deleteLearningOutcomes(
            @PathVariable UUID courseId,
            @Valid @RequestBody DeleteLearningOutcomesRequest request) {
        DeleteLearningOutcomesCommand command = deleteLearningOutcomesCommandMapper.toCommand(courseId, request);
        DeleteLearningOutcomesResponse response = teacherDeleteLearningOutcomesUseCase.deleteLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add learning outcomes to a course", description = "Add one or more learning outcomes to a course. Only the course owner (teacher) can add learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Learning outcomes added to course successfully",
                    content = @Content(schema = @Schema(implementation = CreateLearningOutcomesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to add learning outcomes to this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/learning-outcome/add")
    public ResponseEntity<CreateLearningOutcomesResponse> addLearningOutcomesToCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateLearningOutcomesRequest request) {
        CreateLearningOutcomesCommand command = createLearningOutcomesCommandMapper.toCommand(courseId, request);
        CreateLearningOutcomesResponse response = teacherCreateLearningOutcomesUseCase.createLearningOutcomes(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove learning outcomes from a course", description = "Remove one or more learning outcomes from a course by their IDs. Only the course owner (teacher) can remove learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes removed from course successfully",
                    content = @Content(schema = @Schema(implementation = DeleteLearningOutcomesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course or learning outcome not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to remove learning outcomes from this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/learning-outcome/remove")
    public ResponseEntity<DeleteLearningOutcomesResponse> removeLearningOutcomesFromCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody DeleteLearningOutcomesRequest request) {
        DeleteLearningOutcomesCommand command = deleteLearningOutcomesCommandMapper.toCommand(courseId, request);
        DeleteLearningOutcomesResponse response = teacherDeleteLearningOutcomesUseCase.deleteLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }

    // ==================== CHAPTER-LEVEL LEARNING OUTCOME MANAGEMENT ====================

    @Operation(summary = "Add learning outcomes to a chapter",
            description = "Add learning outcomes to a chapter by their IDs. Only the course owner (teacher) can add learning outcomes. Learning outcomes must belong to the same course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes added to chapter successfully",
                    content = @Content(schema = @Schema(implementation = AddLearningOutcomesToChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course, chapter, or learning outcomes not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/chapter/{chapterNumber}/learning-outcomes/add")
    public ResponseEntity<AddLearningOutcomesToChapterResponse> addLearningOutcomesToChapter(
            @PathVariable UUID courseId,
            @PathVariable Long chapterNumber,
            @Valid @RequestBody AddLearningOutcomesToChapterRequest request) {
        AddLearningOutcomesToChapterCommand command = addLearningOutcomesToChapterCommandMapper.toCommand(courseId, chapterNumber, request);
        AddLearningOutcomesToChapterResponse response = teacherAddLearningOutcomesToChapterUseCase.addLearningOutcomesToChapter(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove learning outcomes from a chapter",
            description = "Remove learning outcomes from a chapter by their IDs. Only the course owner (teacher) can remove learning outcomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes removed from chapter successfully",
                    content = @Content(schema = @Schema(implementation = RemoveLearningOutcomesFromChapterResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course, chapter, or learning outcomes not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this course")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/{courseId}/chapter/{chapterNumber}/learning-outcomes/remove")
    public ResponseEntity<RemoveLearningOutcomesFromChapterResponse> removeLearningOutcomesFromChapter(
            @PathVariable UUID courseId,
            @PathVariable Long chapterNumber,
            @Valid @RequestBody RemoveLearningOutcomesFromChapterRequest request) {
        RemoveLearningOutcomesFromChapterCommand command = removeLearningOutcomesFromChapterCommandMapper.toCommand(courseId, chapterNumber, request);
        RemoveLearningOutcomesFromChapterResponse response = teacherRemoveLearningOutcomesFromChapterUseCase.removeLearningOutcomesFromChapter(command);
        return ResponseEntity.ok(response);
    }
}
