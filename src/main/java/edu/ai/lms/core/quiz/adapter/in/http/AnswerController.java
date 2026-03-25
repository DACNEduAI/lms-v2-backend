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
@RequestMapping("/api/quiz/{questionId}/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final CreateAnswerUseCase createAnswerUseCase;
    private final ModifyAnswerUseCase modifyAnswerUseCase;
    private final DeleteAnswerUseCase deleteAnswerUseCase;
    private final CreateAnswerCommandMapper createAnswerCommandMapper;
    private final ModifyAnswerCommandMapper modifyAnswerCommandMapper;

        @Operation(summary = "Create a new answer", description = "Creates an answer option for a question. LMS logic: defines selectable choices used in quiz grading. Path field: questionId. Request fields: content, isCorrect, explanation. Response fields: answerNumber, content, isCorrect, explanation, questionId, createdAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Answer created successfully",
                    content = @Content(schema = @Schema(implementation = CreateAnswerResponse.class)))
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PostMapping()
    public ResponseEntity<CreateAnswerResponse> createAnswer(
            @PathVariable UUID questionId,
            @Valid @RequestBody CreateAnswerRequest request) {
        CreateAnswerCommand command = createAnswerCommandMapper.toCommand(questionId, request);
        CreateAnswerResponse response = createAnswerUseCase.createAnswer(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        @Operation(summary = "Modify an answer", description = "Updates an answer option. LMS logic: lets quiz creators refine answer correctness/content/feedback. Path fields: questionId, answerNumber. Request fields: content, isCorrect, explanation. Response fields: answerNumber, content, isCorrect, explanation, questionId, updatedAt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Answer modified successfully",
                    content = @Content(schema = @Schema(implementation = ModifyAnswerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Answer not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @PutMapping("/{answerNumber}")
    public ResponseEntity<ModifyAnswerResponse> modifyAnswer(
            @PathVariable UUID questionId,
            @PathVariable Long answerNumber,
            @Valid @RequestBody ModifyAnswerRequest request) {
        ModifyAnswerCommand command = modifyAnswerCommandMapper.toCommand(questionId, answerNumber, request);
        ModifyAnswerResponse response = modifyAnswerUseCase.modifyAnswer(command);
        return ResponseEntity.ok(response);
    }

        @Operation(summary = "Delete an answer", description = "Deletes an answer option from a question. LMS logic: removes obsolete choices from question configuration. Path fields: questionId, answerNumber. Response fields: answerNumber, questionId, message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Answer deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteAnswerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Answer not found")
    })
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @DeleteMapping("/{answerNumber}")
    public ResponseEntity<DeleteAnswerResponse> deleteAnswer(
            @PathVariable UUID questionId,
            @PathVariable Long answerNumber) {
        DeleteAnswerResponse response = deleteAnswerUseCase.deleteAnswer(questionId, answerNumber);
        return ResponseEntity.ok(response);
    }
}
