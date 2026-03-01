package edu.ai.lms.core.quiz.adapter.in.http.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAnswerResponse {
    private Long answerNumber;
    private String content;
    private boolean isCorrect;
    private String explanation;
    private UUID questionId;
    private LocalDateTime createdAt;
}
