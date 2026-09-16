package edu.ai.lms.core.attempt.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptQuestionResult {
    private UUID attemptId;
    private UUID quizId;
    private UUID questionId;
    private Long ktQuestionId;
    private Integer interactionOrder;
    private Boolean correct;
    private UUID learningOutcomeId;
    private Long ktLearningOutcomeId;
    private Integer learningOutcomeMappingCount;
    private LocalDateTime submittedTime;
}
