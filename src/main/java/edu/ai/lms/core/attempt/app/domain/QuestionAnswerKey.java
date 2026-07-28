package edu.ai.lms.core.attempt.app.domain;

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
public class QuestionAnswerKey {

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        ESSAY,
        CODE_RUNNER
    }

    private UUID questionId;

    private QuestionType questionType;

    private Long correctAnswerNumber;
}
