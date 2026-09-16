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
    private String questionType;
    private String questionContent;
    private String questionContentFormat;
    private Long studentAnswerNumber;
    private Integer studentAnswerPosition;
    private String studentAnswerContent;
    private String studentAnswerContentFormat;
    private Long correctAnswerNumber;
    private Integer correctAnswerPosition;
    private String correctAnswerContent;
    private String correctAnswerContentFormat;
    private Boolean correct;
    private UUID learningOutcomeId;
    private Long ktLearningOutcomeId;
    private Integer learningOutcomeMappingCount;
    private Boolean snapshotComplete;
    private LocalDateTime submittedTime;
}
