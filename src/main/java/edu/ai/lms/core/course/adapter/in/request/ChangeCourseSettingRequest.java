package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeCourseSettingRequest {
    @NotBlank(message="Course ID cannot be blank")
    private UUID courseId;

    private String courseName;

    private String description;

    private Boolean active;

    private String coverImage;

    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
    private LocalDate startDate;

    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
    private LocalDate endDate;
}
