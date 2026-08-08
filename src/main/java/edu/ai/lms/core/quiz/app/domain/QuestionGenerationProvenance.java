package edu.ai.lms.core.quiz.app.domain;

import java.time.LocalDateTime;
import java.util.UUID;

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
public class QuestionGenerationProvenance {
    private UUID questionId;
    private UUID generationJobId;
    private UUID matrixRowId;
    private UUID ccctItemId;
    private UUID customLearningLevelId;
    private Question.QuestionType questionType;
    private String allocationSnapshot;
    private LocalDateTime createdAt;
}
