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
public class QuestionLearningOutcomeView {

    private UUID learningOutcomeId;

    private String learningOutcomeCode;

    private String learningOutcomeDescription;
}
