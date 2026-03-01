package edu.ai.lms.core.quiz.adapter.in.http.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionResponse {
    private UUID questionId;
    private String content;
    private String type;
    private Double points;
    private UUID quizId;
    private LocalDateTime createdAt;
}
