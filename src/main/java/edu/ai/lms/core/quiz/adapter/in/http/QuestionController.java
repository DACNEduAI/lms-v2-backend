package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

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

import edu.ai.lms.core.quiz.adapter.in.http.mapper.CreateQuestionCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.mapper.ModifyQuestionCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuestionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.DeleteQuestionsRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuestionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuestionLearningOutcomeOperationRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionLearningOutcomesResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuestionLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.command.RemoveQuestionLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.AddQuestionLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuestionLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.RemoveQuestionLearningOutcomesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/quiz/{quizId}/question")
@RequiredArgsConstructor
public class QuestionController {

    private final CreateQuestionUseCase createQuestionUseCase;
    private final ModifyQuestionUseCase modifyQuestionUseCase;
    private final DeleteQuestionUseCase deleteQuestionUseCase;
    private final GetQuestionUseCase getQuestionUseCase;
    private final GetQuestionLearningOutcomesUseCase getQuestionLearningOutcomesUseCase;
    private final AddQuestionLearningOutcomesUseCase addQuestionLearningOutcomesUseCase;
    private final RemoveQuestionLearningOutcomesUseCase removeQuestionLearningOutcomesUseCase;
    private final CreateQuestionCommandMapper createQuestionCommandMapper;
    private final ModifyQuestionCommandMapper modifyQuestionCommandMapper;

        @Operation(summary = "Get learning outcomes of a question", description = "Returns learning outcomes mapped to a question. LMS logic: enables question-level CLO traceability. Path fields: quizId, questionId. Response fields: questionId, quizId, courseId, learningOutcomes[] (id, code, description).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetQuestionLearningOutcomesResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{questionId}/learning-outcomes")
    public ResponseEntity<GetQuestionLearningOutcomesResponse> getQuestionLearningOutcomes(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        GetQuestionLearningOutcomesResponse response = getQuestionLearningOutcomesUseCase
                .getLearningOutcomes(quizId, questionId);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Add learning outcomes to a question", description = "Adds outcome mappings to a question. LMS logic: aligns assessment items with learning objectives. Path fields: quizId, questionId. Request fields: learningOutcomeIds[]. Response fields: questionId, quizId, courseId, operation, totalAffected, learningOutcomeIds[].")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes added successfully",
                    content = @Content(schema = @Schema(implementation = QuestionLearningOutcomeOperationResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping("/{questionId}/learning-outcomes")
    public ResponseEntity<QuestionLearningOutcomeOperationResponse> addQuestionLearningOutcomes(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionLearningOutcomeOperationRequest request) {
        AddQuestionLearningOutcomesCommand command = AddQuestionLearningOutcomesCommand.builder()
                .quizId(quizId)
                .questionId(questionId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();

        QuestionLearningOutcomeOperationResponse response = addQuestionLearningOutcomesUseCase
                .addLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Remove learning outcomes from a question", description = "Removes outcome mappings from a question. LMS logic: updates objective alignment after content changes. Path fields: quizId, questionId. Request fields: learningOutcomeIds[]. Response fields: questionId, quizId, courseId, operation, totalAffected, learningOutcomeIds[].")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes removed successfully",
                    content = @Content(schema = @Schema(implementation = QuestionLearningOutcomeOperationResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping("/{questionId}/learning-outcomes")
    public ResponseEntity<QuestionLearningOutcomeOperationResponse> removeQuestionLearningOutcomes(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionLearningOutcomeOperationRequest request) {
        RemoveQuestionLearningOutcomesCommand command = RemoveQuestionLearningOutcomesCommand.builder()
                .quizId(quizId)
                .questionId(questionId)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();

        QuestionLearningOutcomeOperationResponse response = removeQuestionLearningOutcomesUseCase
                .removeLearningOutcomes(command);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Get a specific question", description = "Gets question detail including answer options. LMS logic: used for quiz rendering and editing flows. Path fields: quizId, questionId. Response fields: questionId, content, type, points, quizId, answers[] (answerNumber, content, isCorrect, explanation), createdAt, updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{questionId}")
    public ResponseEntity<GetQuestionResponse> getQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        GetQuestionResponse response = getQuestionUseCase.getQuestion(quizId, questionId);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Create questions", description = "Creates one or many questions in a quiz. LMS logic: supports batch authoring with nested answer options. Path field: quizId. Request fields: questions[] (content, type, points, answers[]). Response fields per item: questionId, content, type, points, quizId, answers[], createdAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Question created successfully",
                    content = @Content(schema = @Schema(implementation = CreateQuestionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping()
    public ResponseEntity<List<CreateQuestionResponse>> createQuestion(
            @PathVariable UUID quizId,
            @Valid @RequestBody CreateQuestionRequest request) {
        List<CreateQuestionResponse> responses = request.getQuestions().stream()
                .map(questionRequest -> {
                    CreateQuestionCommand command = createQuestionCommandMapper.toCommand(quizId, questionRequest);
                    return createQuestionUseCase.createQuestion(command);
                })
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

        @Operation(summary = "Modify questions", description = "Updates one or many questions in batch. LMS logic: allows quiz creators to revise question text/type/points/answers. Path field: quizId. Request fields: questions[] with required questionId + updated fields. Response fields per item: questionId, content, type, points, quizId, answers[], updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PutMapping()
    public ResponseEntity<List<ModifyQuestionResponse>> modifyQuestion(
            @PathVariable UUID quizId,
            @Valid @RequestBody ModifyQuestionRequest request) {
        List<ModifyQuestionResponse> responses = request.getQuestions().stream()
                .map(questionRequest -> {
                    ModifyQuestionCommand command = modifyQuestionCommandMapper.toCommand(quizId, questionRequest);
                    return modifyQuestionUseCase.modifyQuestion(command);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

        @Operation(summary = "Delete a question", description = "Deletes a single question from a quiz. LMS logic: removes one assessment item. Path fields: quizId, questionId. Response fields: questionId, message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping("/{questionId}")
    public ResponseEntity<DeleteQuestionResponse> deleteQuestion(
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        DeleteQuestionResponse response = deleteQuestionUseCase.deleteQuestion(quizId, questionId);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Delete questions", description = "Batch deletes questions from a quiz. LMS logic: bulk question cleanup. Path field: quizId. Request fields: questionIds[]. Response: list of delete results (questionId, message).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Questions deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping()
    public ResponseEntity<List<DeleteQuestionResponse>> deleteQuestions(
            @PathVariable UUID quizId,
            @Valid @RequestBody DeleteQuestionsRequest request) {
        List<DeleteQuestionResponse> responses = request.getQuestionIds().stream()
                .map(questionId -> deleteQuestionUseCase.deleteQuestion(quizId, questionId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
