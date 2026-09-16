package edu.ai.lms.core.attempt.adapter.in.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ai.lms.config.DatePatternConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyAttemptsResponse {

    private UUID quizId;

    @Builder.Default
    private List<AttemptItem> attempts = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttemptItem {

        private UUID attemptId;

        private String status;

        private Double score;

        @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
        private LocalDateTime startTime;

        @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
        private LocalDateTime dueTime;

        @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
        private LocalDateTime submittedTime;

        private Integer timeSpentSeconds;

        private String errorReason;

        @Builder.Default
        private List<AttemptRecordItem> attemptRecords = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttemptRecordItem {
        private UUID questionId;
        private int interactionOrder;
        private String questionType;
        private String questionContent;
        private String questionContentFormat;
        private UUID learningOutcomeId;
        private String learningOutcomeCode;
        private Integer learningOutcomeMappingCount;
        private AnswerItem studentAnswer;
        private AnswerItem correctAnswer;
        @JsonProperty("isCorrect")
        private boolean isCorrect;
        private boolean snapshotComplete;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerItem {
        private Long answerNumber;
        private Integer displayedPosition;
        private String displayedLabel;
        private String content;
        private String contentFormat;
    }
}
