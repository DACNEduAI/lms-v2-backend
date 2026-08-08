package edu.ai.lms.core.quiz.adapter.in.http.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz.app.domain.QuizSourceType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetAllQuizzesResponse {

    private List<QuizSummary> quizzes;
    private int totalQuizzes;
    private int pageSize;
    private String lastCreated;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizSummary {
        private UUID quizId;
        private String quizTitle;
        private String description;
        private Boolean unlimited;
        private Integer timeLimitMinutes;
        private Double passScore;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
        private LocalDateTime startTime;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
        private LocalDateTime dueTime;
        private Integer attemptLimited;
        private Boolean display;
        private QuizLifecycleStatus lifecycleStatus;
        private QuizSourceType sourceType;
        private UUID originalQuizId;
        private long version;
        private UUID publishedBy;
        private LocalDateTime publishedAt;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
        private LocalDateTime createdAt;
    }
}
