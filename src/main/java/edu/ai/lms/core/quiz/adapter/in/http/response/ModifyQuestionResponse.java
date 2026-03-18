package edu.ai.lms.core.quiz.adapter.in.http.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyQuestionResponse {
    private UUID questionId;
    private String content;
    private String type;
    private Double points;
    private UUID quizId;
    private List<AnswerSummary> answers;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerSummary {
        private Long answerNumber;
        private String content;
        private boolean isCorrect;
        private String explanation;
    }
}
