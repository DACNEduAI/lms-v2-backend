package edu.ai.lms.core.quiz.app.domain.questionbank;

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
public class QuestionBankAnswer {
    private UUID id;
    private UUID questionId;
    private String content;
    private boolean correct;
    private String explanation;
    private int orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
