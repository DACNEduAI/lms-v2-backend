package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.ai.lms.config.DatePatternConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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

    @Builder.Default
    private List<UUID> selectedMaterialIds = new ArrayList<>();

    private String prompt;

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
}
