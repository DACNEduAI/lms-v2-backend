package edu.ai.lms.core.quiz.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizLearningOutcomeView {
    private UUID learningOutcomeId;
    private String learningOutcomeCode;
    private Integer learningOutcomePrefixNumber;
    private String learningOutcomeDescription;
    private UUID groupLearningOutcomeId;
    private String groupLearningOutcomeName;
    private String groupLearningOutcomeDescription;
}
