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
public class CreateQuizResponse {
    private UUID quizId;
    private String quizTitle;
    private Integer timeLimitMinutes;
    private Double passScore;
    private UUID courseId;
    private Boolean active;
    private List<CreatedQuestion> questions;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatedQuestion {
        private UUID questionId;
        private String content;
        private String type;
        private Double points;
        private List<CreatedAnswer> answers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatedAnswer {
        private Long answerNumber;
        private String content;
        private boolean isCorrect;
        private String explanation;
    }
}
