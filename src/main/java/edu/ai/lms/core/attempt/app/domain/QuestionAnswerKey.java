package edu.ai.lms.core.attempt.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAnswerKey {

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        ESSAY,
        CODE_RUNNER
    }

    private UUID questionId;

    private Long ktQuestionId;

    private QuestionType questionType;

    private String questionContent;

    private String questionContentFormat;

    @Builder.Default
    private List<AnswerSnapshot> answers = List.of();

    @Builder.Default
    private List<LearningOutcomeSnapshot> learningOutcomes = List.of();

    public record LearningOutcomeSnapshot(UUID learningOutcomeId, long ktLearningOutcomeId) {
    }

    public record AnswerSnapshot(long answerNumber, String content, String contentFormat, boolean correct) {
    }
}
