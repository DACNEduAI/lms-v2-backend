package edu.ai.lms.core.quiz.app.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.common.Roles;
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

    private UUID id;

    private UUID courseId;

    private UUID chapterId;

    private UUID quizId;

    private UUID requestedBy;

    private Roles requestedRole;

    private String quizTitle;

    private Integer questionCount;

    private String prompt;

    private Boolean useOptions;

    private Boolean unlimited;

    private Integer timeLimitMinutes;

    private Double passScore;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private Integer attemptLimited;

    private Boolean display;

    private QuizGenerationStatus status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
