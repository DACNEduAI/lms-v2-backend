package edu.ai.lms.core.quiz.app.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    // Answer is a weak entity - its identity is (answerNumber, questionId)
    private Long answerNumber; // Auto-increment within the question context

    private String content;

    private boolean isCorrect;

    private String explanation;

    private UUID questionId; // Part of composite primary key

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
