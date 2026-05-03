package edu.ai.lms.core.quiz.adapter.in.http.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuizRequest {

    @NotBlank(message = "Quiz title cannot be blank")
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

    @NotEmpty(message = "Learning outcomes are required")
    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();
}
