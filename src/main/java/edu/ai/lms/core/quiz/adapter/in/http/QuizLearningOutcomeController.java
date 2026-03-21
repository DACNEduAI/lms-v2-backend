package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.adapter.in.http.request.QuizLearningOutcomeOperationRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.command.RemoveQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.AddQuizLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.RemoveQuizLearningOutcomesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/quiz/{quizId}/learning-outcomes")
@RequiredArgsConstructor
public class QuizLearningOutcomeController {

    private final AddQuizLearningOutcomesUseCase addQuizLearningOutcomesUseCase;
    private final RemoveQuizLearningOutcomesUseCase removeQuizLearningOutcomesUseCase;

    @Operation(summary = "Add learning outcomes to a quiz",
            description = "Adds learning outcomes by explicit ids and/or group ids. Group ids are expanded to child learning outcomes and overlaps are deduplicated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes added successfully",
                    content = @Content(schema = @Schema(implementation = QuizLearningOutcomeOperationResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping
    public ResponseEntity<QuizLearningOutcomeOperationResponse> addLearningOutcomes(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @Valid @RequestBody QuizLearningOutcomeOperationRequest request) {

        AddQuizLearningOutcomesCommand command = AddQuizLearningOutcomesCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .groupLearningOutcomeIds(request.getGroupLearningOutcomeIds())
                .build();

        QuizLearningOutcomeOperationResponse response = addQuizLearningOutcomesUseCase.addLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove learning outcomes from a quiz",
            description = "Removes learning outcomes by explicit ids and/or group ids. Group ids are expanded to child learning outcomes and overlaps are deduplicated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes removed successfully",
                    content = @Content(schema = @Schema(implementation = QuizLearningOutcomeOperationResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping
    public ResponseEntity<QuizLearningOutcomeOperationResponse> removeLearningOutcomes(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @Valid @RequestBody QuizLearningOutcomeOperationRequest request) {

        RemoveQuizLearningOutcomesCommand command = RemoveQuizLearningOutcomesCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .groupLearningOutcomeIds(request.getGroupLearningOutcomeIds())
                .build();

        QuizLearningOutcomeOperationResponse response = removeQuizLearningOutcomesUseCase.removeLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }
}
