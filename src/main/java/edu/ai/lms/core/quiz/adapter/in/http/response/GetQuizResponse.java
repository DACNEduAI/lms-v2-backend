package edu.ai.lms.core.quiz.adapter.in.http.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
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
public class GetQuizResponse {
    private UUID quizId;
    private String quizTitle;
    private Integer timeLimitMinutes;
    private Double passScore;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime startTime;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime dueTime;
    private Integer attemptLimited;
    private UUID courseId;
    private Boolean display;
    @Builder.Default
    private List<LearningOutcomeSummary> ungroupedLearningOutcomes = new ArrayList<>();
    @Builder.Default
    private List<GroupLearningOutcomeSummary> groupedLearningOutcomes = new ArrayList<>();
    private List<QuestionSummary> questions;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime createdAt;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionSummary {
        private UUID questionId;
        private String content;
        private String type;
        private Double points;
        private List<AnswerSummary> answers;
    }

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeSummary {
        private UUID id;
        private String code;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupLearningOutcomeSummary {
        private UUID id;
        private String name;
        private String description;
        @Builder.Default
        private List<LearningOutcomeSummary> learningOutcomes = new ArrayList<>();
    }
}
