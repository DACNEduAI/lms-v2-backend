package edu.ai.lms.core.quiz.app.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        ESSAY
    }

    private UUID id;

    private String content;

    private QuestionType type;

    private Double points;

    private UUID quizId;

    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
