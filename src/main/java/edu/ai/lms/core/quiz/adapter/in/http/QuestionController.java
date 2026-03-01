package edu.ai.lms.core.quiz.adapter.in.http;

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

import edu.ai.lms.core.quiz.adapter.in.http.mapper.CreateQuestionCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.mapper.ModifyQuestionCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuestionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuestionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuestionResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuestionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/quiz/{quizId}/question")
@RequiredArgsConstructor
public class QuestionController {

    private final CreateQuestionUseCase createQuestionUseCase;
    private final ModifyQuestionUseCase modifyQuestionUseCase;
    private final DeleteQuestionUseCase deleteQuestionUseCase;
    private final GetQuestionUseCase getQuestionUseCase;
    private final CreateQuestionCommandMapper createQuestionCommandMapper;
    private final ModifyQuestionCommandMapper modifyQuestionCommandMapper;

    @Operation(summary = "Get a specific question", description = "Get a question with its answers by question ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GetQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/{questionId}")
    public ResponseEntity<GetQuestionResponse> getQuestion(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        GetQuestionResponse response = getQuestionUseCase.getQuestion(quizId, questionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new question", description = "Add a question to a quiz. Only the quiz creator can add questions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Question created successfully",
                    content = @Content(schema = @Schema(implementation = CreateQuestionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping()
    public ResponseEntity<CreateQuestionResponse> createQuestion(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @Valid @RequestBody CreateQuestionRequest request) {
        CreateQuestionCommand command = createQuestionCommandMapper.toCommand(quizId, request);
        CreateQuestionResponse response = createQuestionUseCase.createQuestion(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Modify a question", description = "Update a question. Only the quiz creator can modify questions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PutMapping("/{questionId}")
    public ResponseEntity<ModifyQuestionResponse> modifyQuestion(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody ModifyQuestionRequest request) {
        ModifyQuestionCommand command = modifyQuestionCommandMapper.toCommand(quizId, questionId, request);
        ModifyQuestionResponse response = modifyQuestionUseCase.modifyQuestion(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a question", description = "Delete a question from a quiz. Only the quiz creator can delete questions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteQuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping("/{questionId}")
    public ResponseEntity<DeleteQuestionResponse> deleteQuestion(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        DeleteQuestionResponse response = deleteQuestionUseCase.deleteQuestion(quizId, questionId);
        return ResponseEntity.ok(response);
    }
}
