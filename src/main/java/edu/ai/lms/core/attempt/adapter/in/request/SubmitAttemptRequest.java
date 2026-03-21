package edu.ai.lms.core.attempt.adapter.in.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAttemptRequest {

    @NotNull(message = "quizId is required")
    private UUID quizId;

    @Valid
    private List<SubmittedAnswerRequest> submittedAnswers;

    @Min(value = 0, message = "timeSpentSeconds must be >= 0")
    private Integer timeSpentSeconds;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmittedAnswerRequest {

        @NotNull(message = "questionId is required")
        private UUID questionId;

        @NotNull(message = "studentAnswerId is required")
        private Long studentAnswerId;
    }
}
