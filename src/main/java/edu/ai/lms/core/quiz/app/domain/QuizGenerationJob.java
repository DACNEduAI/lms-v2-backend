package edu.ai.lms.core.quiz.app.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;

import edu.ai.lms.common.api.Roles;
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
public class QuizGenerationJob {
    public enum Phase { TEXT_GENERATION, VISUAL_GENERATION, FINALIZING, DONE }

    private UUID id;

    private UUID courseId;

    private UUID examMatrixId;

    private UUID chapterId;

    private UUID quizId;

    private UUID requestedBy;

    private Roles requestedRole;

    private String quizTitle;

    private Integer questionCount;

    private Question.QuestionType questionType;

    private String prompt;

    private String requestSnapshot;

    private String payloadHash;

    private String idempotencyKey;

    private Boolean strictMode;

    private String codeRunnerPolicy;

    private Boolean useOptions;

    private Boolean unlimited;

    private Integer timeLimitMinutes;

    private Double passScore;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private Integer attemptLimited;

    private Boolean display;

    private QuizGenerationStatus status;

    private Phase phase;

    private Integer attemptCount;

    private LocalDateTime nextAttemptAt;

    private LocalDateTime leaseUntil;

    private String errorCode;

    private String errorMessage;

    private UUID callbackEventId;

    private LocalDateTime createdAt;

    private LocalDateTime processingAt;

    private LocalDateTime completedAt;

    private LocalDateTime updatedAt;
}
