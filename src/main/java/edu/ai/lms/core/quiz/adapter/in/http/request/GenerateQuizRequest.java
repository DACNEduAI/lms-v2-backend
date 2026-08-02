package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.ai.lms.config.DatePatternConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
public class GenerateQuizRequest {

    @NotNull(message = "CourseId is required")
    private UUID courseId;

    @NotNull(message = "Exam matrix is required")
    private UUID examMatrixId;

    @Valid
    @Builder.Default
    private List<VisualRequirement> visualRequirements = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<QuestionTypeOverride> questionTypeOverrides = new ArrayList<>();

    @Builder.Default
    private List<UUID> selectedMaterialIds = new ArrayList<>();

    private String prompt;

    @Valid
    @Builder.Default
    private QuestionDefaults questionDefaults = new QuestionDefaults();

    @Builder.Default
    private Boolean strictMode = true;

    @Builder.Default
    private String codeRunnerPolicy = "PRESERVE";

    private String quizTitle;

    private Boolean unlimited;

    @Positive(message = "Time limit must be greater than 0 minutes")
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisualRequirement {
        @NotNull
        private UUID examMatrixRowId;
        @NotNull
        private UUID customLearningLevelId;
        @Min(0)
        private int imageQuestionCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionTypeOverride {
        @NotNull
        private UUID examMatrixRowId;
        @NotNull
        private UUID customLearningLevelId;
        @NotNull
        @Pattern(regexp = "MULTIPLE_CHOICE|ESSAY|CODE_RUNNER")
        private String questionType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionDefaults {
        @Valid
        @Builder.Default
        private MultipleChoiceDefaults multipleChoice = new MultipleChoiceDefaults();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MultipleChoiceDefaults {
        @Min(value = 4, message = "Answer option count must be at least 4")
        @Max(value = 10, message = "Answer option count must not exceed 10")
        @Builder.Default
        private Integer answerOptionCount = 4;

        @Builder.Default
        private Boolean allowMultipleCorrectAnswers = false;

        @Builder.Default
        private Boolean requireAnswerExplanation = true;
    }
}
