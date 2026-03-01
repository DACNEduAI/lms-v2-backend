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
public class GetAllQuizzesResponse {

    private List<QuizSummary> quizzes;
    private int totalQuizzes;
    private int pageSize;
    private String sortDirection;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizSummary {
        private UUID quizId;
        private String quizTitle;
        private Integer timeLimitMinutes;
        private Double passScore;
        private Boolean active;
        private LocalDateTime createdAt;
    }
}
