package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizGenerationStatusResponse {

    private UUID jobId;

    private QuizGenerationStatus status;

    private QuizGenerationJob.Phase phase;

    private VisualSummary visualSummary;

    private UUID quizId;

    private String errorMessage;

    private String errorCode;

    private Integer attemptCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public record VisualSummary(int requested, int ready, int failed) {}
}
