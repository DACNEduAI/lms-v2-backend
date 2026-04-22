package edu.ai.lms.core.attempt.adapter.in;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/{courseId}/attempt")
@Tag(name = "Attempt Controller", description = "Endpoints for starting, submitting, and reviewing student quiz attempts.")
@RequiredArgsConstructor
public class AttemptController {

    private final StartAttemptUseCase startAttemptUseCase;
    private final SubmitAttemptUseCase submitAttemptUseCase;
    private final GetMyAttemptsUseCase getMyAttemptsUseCase;
    private final SubmitAttemptCommandMapper submitAttemptCommandMapper;

    @Operation(
            summary = "Start a quiz attempt",
            description = """
                    Starts a quiz attempt for the authenticated student.

                    LMS business logic:
                    - Creates an attempt record in STARTED state.
                    - Applies quiz timing policy (start window/due time) to the attempt lifecycle.

                    Path fields:
                    - courseId: Course that contains the quiz.
                    - quizId: Quiz to attempt.

                    Response fields:
                    - attemptId: Created attempt identifier.
                    - quizId: Quiz identifier.
                    - status: Attempt lifecycle status.
                    - startTime: Attempt start timestamp.
                    - dueTime: Attempt deadline timestamp.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempt started successfully",
                    content = @Content(schema = @Schema(implementation = StartAttemptResponse.class)))
    })
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/quiz/{quizId}/start")
    public ResponseEntity<StartAttemptResponse> startAttempt(
            @Parameter(description = "Course ID that owns the quiz") @PathVariable UUID courseId,
            @Parameter(description = "Quiz ID to start an attempt for") @PathVariable UUID quizId) {
        StartAttemptCommand command = StartAttemptCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .build();

        StartAttemptResponse response = startAttemptUseCase.startAttempt(command);
        return ResponseEntity.ok(response);
    }

                @Operation(
                                                summary = "Submit a quiz attempt",
                                                description = """
                                                                                Submits answers for an active attempt.

                                                                                LMS business logic:
                                                                                - Validates request quizId against URL quizId.
                                                                                - Evaluates submitted answers and finalizes score/status.

                                                                                Path fields:
                                                                                - courseId: Course context.
                                                                                - quizId: Quiz being submitted.

                                                                                Request body fields:
                                                                                - quizId: Must match the quizId in the URL.
                                                                                - submittedAnswers[]: Student answer payload list.
                                                                                        - questionId: Question being answered.
                                                                                        - studentAnswerId: Selected answer number/ID.
                                                                                - timeSpentSeconds: Total attempt duration in seconds.

                                                                                Response fields:
                                                                                - attemptId: Submitted attempt identifier.
                                                                                - quizId: Quiz identifier.
                                                                                - status: Final attempt status.
                                                                                - score: Computed score.
                                                                                - timeSpentSeconds: Stored attempt duration.
                                                                                - submittedTime: Submission timestamp.
                                                                                - dueTime: Deadline timestamp used for validation.
                                                                                - errorReason: Reason for invalid/late submission if present.
                                                                                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempt submitted",
                    content = @Content(schema = @Schema(implementation = SubmitAttemptResponse.class)))
    })
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/quiz/{quizId}/submit")
    public ResponseEntity<SubmitAttemptResponse> submitAttempt(
            @Parameter(description = "Course ID that owns the quiz") @PathVariable UUID courseId,
            @Parameter(description = "Quiz ID being submitted") @PathVariable UUID quizId,
            @Valid @RequestBody SubmitAttemptRequest request) {

                if (!quizId.equals(request.getQuizId())) {
                        throw new InvalidAttemptSubmissionException("quizId in request body must match quizId in URL");
                }

        SubmitAttemptCommand command = submitAttemptCommandMapper.toCommand(courseId, quizId, request);
        SubmitAttemptResponse response = submitAttemptUseCase.submitAttempt(command);
        return ResponseEntity.ok(response);
    }

                @Operation(
                                                summary = "Get my attempts by quiz",
                                                description = """
                                                                                Returns all attempts of the authenticated student for a specific quiz.

                                                                                LMS business logic:
                                                                                - Provides learner attempt history for progress, scoring, and retry tracking.

                                                                                Path fields:
                                                                                - courseId: Course context.
                                                                                - quizId: Quiz to fetch attempt history for.

                                                                                Response fields:
                                                                                - quizId: Quiz identifier.
                                                                                - attempts[]: Attempt history list.
                                                                                        - attemptId: Attempt identifier.
                                                                                        - status: Attempt status.
                                                                                        - score: Attempt score.
                                                                                        - startTime: Attempt start time.
                                                                                        - dueTime: Attempt deadline.
                                                                                        - submittedTime: Attempt submission time.
                                                                                        - timeSpentSeconds: Duration spent.
                                                                                        - errorReason: Late/error reason if any.
                                                                                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attempts fetched",
                    content = @Content(schema = @Schema(implementation = GetMyAttemptsResponse.class)))
    })
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/quiz/{quizId}/me")
    public ResponseEntity<GetMyAttemptsResponse> getMyAttempts(
                        @Parameter(description = "Course ID that owns the quiz") @PathVariable UUID courseId,
                        @Parameter(description = "Quiz ID to fetch my attempts") @PathVariable UUID quizId) {
        return ResponseEntity.ok(getMyAttemptsUseCase.getMyAttempts(courseId, quizId));
    }
}
