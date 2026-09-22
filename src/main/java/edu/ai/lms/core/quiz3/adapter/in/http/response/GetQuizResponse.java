package edu.ai.lms.core.quiz3.adapter.in.http.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz3.app.domain.QuestionSourceType;
import edu.ai.lms.core.quiz3.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz3.app.domain.QuizSourceType;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetQuizResponse {
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
    private UUID courseId;
    private Boolean display;
    private QuizLifecycleStatus lifecycleStatus;
    private QuizSourceType sourceType;
    private UUID originalQuizId;
    private long version;
    private UUID publishedBy;
    private LocalDateTime publishedAt;
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
        private RichContentFormat contentFormat;
        private String type;
        private QuestionSourceType sourceType;
        private UUID sourceQuestionId;
        private long version;
        private List<LearningOutcomeSummary> learningOutcomes;
        private List<AnswerSummary> answers;
        @Builder.Default
        private List<QuestionImageSummary> images = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionImageSummary {
        private UUID id;
        private String sourceType;
        private String sourceText;
        private String generationBrief;
        private String altText;
        private String status;
        private String imageName;
        private String url;
        private String mediaType;
        private String errorCode;
        private String errorMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerSummary {
        private Long answerNumber;
        private String content;
        private RichContentFormat contentFormat;
        private boolean correct;
        private String explanation;
        private RichContentFormat explanationFormat;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeSummary {
        private UUID id;
        private String code;
        private Integer prefixNumber;
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
