package edu.ai.lms.core.attempt.adapter.in;

import edu.ai.lms.core.attempt.adapter.in.mapper.SubmitAttemptCommandMapper;
import edu.ai.lms.core.attempt.adapter.in.request.SubmitAttemptRequest;
import edu.ai.lms.core.attempt.adapter.in.response.GetMyAttemptsResponse;
import edu.ai.lms.core.attempt.adapter.in.response.StartAttemptResponse;
import edu.ai.lms.core.attempt.adapter.in.response.SubmitAttemptResponse;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.port.in.command.StartAttemptCommand;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;
import edu.ai.lms.core.attempt.app.port.in.usecase.GetMyAttemptsUseCase;
import edu.ai.lms.core.attempt.app.port.in.usecase.StartAttemptUseCase;
import edu.ai.lms.core.attempt.app.port.in.usecase.SubmitAttemptUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{courseId}/attempt")
@RequiredArgsConstructor
public class AttemptController {

    private final StartAttemptUseCase startAttemptUseCase;
    private final SubmitAttemptUseCase submitAttemptUseCase;
    private final GetMyAttemptsUseCase getMyAttemptsUseCase;
    private final SubmitAttemptCommandMapper submitAttemptCommandMapper;

    @Operation(summary = "Start a quiz attempt", description = "Creates a started attempt for the authenticated student.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempt started successfully",
                    content = @Content(schema = @Schema(implementation = StartAttemptResponse.class)))
    })
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/quiz/{quizId}/start")
    public ResponseEntity<StartAttemptResponse> startAttempt(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId) {
        StartAttemptCommand command = StartAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .build();

        StartAttemptResponse response = startAttemptUseCase.startAttempt(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit a quiz attempt", description = "Submits answers for the active started attempt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempt submitted",
                    content = @Content(schema = @Schema(implementation = SubmitAttemptResponse.class)))
    })
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/quiz/{quizId}/submit")
    public ResponseEntity<SubmitAttemptResponse> submitAttempt(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @Valid @RequestBody SubmitAttemptRequest request) {

                if (!quizId.equals(request.getQuizId())) {
                        throw new InvalidAttemptSubmissionException("quizId in request body must match quizId in URL");
                }

        SubmitAttemptCommand command = submitAttemptCommandMapper.toCommand(courseId, quizId, request);
        SubmitAttemptResponse response = submitAttemptUseCase.submitAttempt(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my attempts by quiz", description = "Returns attempts of the authenticated student for the given quiz.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempts fetched",
                    content = @Content(schema = @Schema(implementation = GetMyAttemptsResponse.class)))
    })
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/quiz/{quizId}/me")
    public ResponseEntity<GetMyAttemptsResponse> getMyAttempts(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId) {
        return ResponseEntity.ok(getMyAttemptsUseCase.getMyAttempts(courseId, quizId));
    }
}
