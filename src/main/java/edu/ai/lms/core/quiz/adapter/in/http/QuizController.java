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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.adapter.in.http.mapper.CreateQuizCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.mapper.ModifyQuizCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuizRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.DeleteQuizzesRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuizRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.QuizLearningOutcomeOperationRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.command.RemoveQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.AddQuizLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetAllQuizzesUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuizUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.RemoveQuizLearningOutcomesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final CreateQuizUseCase createQuizUseCase;
    private final ModifyQuizUseCase modifyQuizUseCase;
    private final DeleteQuizUseCase deleteQuizUseCase;
    private final GetQuizUseCase getQuizUseCase;
    private final GetAllQuizzesUseCase getAllQuizzesUseCase;
        private final AddQuizLearningOutcomesUseCase addQuizLearningOutcomesUseCase;
        private final RemoveQuizLearningOutcomesUseCase removeQuizLearningOutcomesUseCase;
    private final CreateQuizCommandMapper createQuizCommandMapper;
    private final ModifyQuizCommandMapper modifyQuizCommandMapper;

        @Operation(summary = "Get teacher-created quizzes in a course", description = "Students see only displayed teacher-created quizzes; teachers see all teacher-created quizzes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetAllQuizzesResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
        @GetMapping("/teacher-created")
    public ResponseEntity<GetAllQuizzesResponse> getAllQuizzes(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "10") int pageSize,
                        @RequestParam(defaultValue = "asc") String lastCreated) {
                GetAllQuizzesResponse response = getAllQuizzesUseCase.getAllQuizzes(courseId, pageSize, lastCreated);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a specific quiz", description = "Get a quiz with full questions and answers by its ID within a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quiz retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetQuizResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found")
    })
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{quizId}")
    public ResponseEntity<GetQuizResponse> getQuiz(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId) {
        GetQuizResponse response = getQuizUseCase.getQuiz(courseId, quizId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new quiz", description = "Create a new quiz in a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Quiz created successfully",
                    content = @Content(schema = @Schema(implementation = CreateQuizResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping()
    public ResponseEntity<CreateQuizResponse> createQuiz(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateQuizRequest request) {
        CreateQuizCommand command = createQuizCommandMapper.toCommand(courseId, request);
        CreateQuizResponse response = createQuizUseCase.createQuiz(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Modify an existing quiz", description = "Update quiz details. Only the quiz creator can modify.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quiz modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyQuizResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PutMapping("/{quizId}")
    public ResponseEntity<ModifyQuizResponse> modifyQuiz(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @Valid @RequestBody ModifyQuizRequest request) {
        ModifyQuizCommand command = modifyQuizCommandMapper.toCommand(courseId, quizId, request);
        ModifyQuizResponse response = modifyQuizUseCase.modifyQuiz(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a quiz", description = "Delete a quiz. Only the quiz creator can delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quiz deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteQuizResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping("/{quizId}")
    public ResponseEntity<DeleteQuizResponse> deleteQuiz(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId) {
        DeleteQuizResponse response = deleteQuizUseCase.deleteQuiz(courseId, quizId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete quizzes", description = "Delete one or many quizzes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quizzes deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteQuizResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping()
    public ResponseEntity<List<DeleteQuizResponse>> deleteQuizzes(
            @PathVariable UUID courseId,
            @Valid @RequestBody DeleteQuizzesRequest request) {
        List<DeleteQuizResponse> responses = request.getQuizIds().stream()
                .map(quizId -> deleteQuizUseCase.deleteQuiz(courseId, quizId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Add learning outcomes to a quiz",
            description = "Adds learning outcomes by explicit ids and/or group ids. Group ids are expanded to child learning outcomes and overlaps are deduplicated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learning outcomes added successfully",
                    content = @Content(schema = @Schema(implementation = QuizLearningOutcomeOperationResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping("/{quizId}/learning-outcomes")
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
    @DeleteMapping("/{quizId}/learning-outcomes")
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
