package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.UUID;

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

import edu.ai.lms.core.quiz.adapter.in.http.mapper.CreateAnswerCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.mapper.ModifyAnswerCommandMapper;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateAnswerRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyAnswerRequest;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateAnswerResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteAnswerResponse;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyAnswerResponse;
import edu.ai.lms.core.quiz.app.port.in.command.CreateAnswerCommand;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyAnswerCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateAnswerUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteAnswerUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyAnswerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/quiz/{quizId}/question/{questionId}/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final CreateAnswerUseCase createAnswerUseCase;
    private final ModifyAnswerUseCase modifyAnswerUseCase;
    private final DeleteAnswerUseCase deleteAnswerUseCase;
    private final CreateAnswerCommandMapper createAnswerCommandMapper;
    private final ModifyAnswerCommandMapper modifyAnswerCommandMapper;

    @Operation(summary = "Create a new answer", description = "Add an answer to a question. Only the quiz creator can add answers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Answer created successfully",
                    content = @Content(schema = @Schema(implementation = CreateAnswerResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping()
    public ResponseEntity<CreateAnswerResponse> createAnswer(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody CreateAnswerRequest request) {
        CreateAnswerCommand command = createAnswerCommandMapper.toCommand(questionId, request);
        CreateAnswerResponse response = createAnswerUseCase.createAnswer(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Modify an answer", description = "Update an answer. Only the quiz creator can modify answers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Answer modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyAnswerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Answer not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PutMapping("/{answerNumber}")
    public ResponseEntity<ModifyAnswerResponse> modifyAnswer(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @PathVariable Long answerNumber,
            @Valid @RequestBody ModifyAnswerRequest request) {
        ModifyAnswerCommand command = modifyAnswerCommandMapper.toCommand(questionId, answerNumber, request);
        ModifyAnswerResponse response = modifyAnswerUseCase.modifyAnswer(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an answer", description = "Delete an answer from a question. Only the quiz creator can delete answers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Answer deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteAnswerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Answer not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping("/{answerNumber}")
    public ResponseEntity<DeleteAnswerResponse> deleteAnswer(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @PathVariable Long answerNumber) {
        DeleteAnswerResponse response = deleteAnswerUseCase.deleteAnswer(questionId, answerNumber);
        return ResponseEntity.ok(response);
    }
}
