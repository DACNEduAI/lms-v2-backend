package edu.ai.lms.core.course.adapter.in.request;

import edu.ai.lms.config.DatePatternConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyCourseRequest {

    @NotBlank(message = "Course name cannot be blank")
    private String courseName;

    private String description;

    @NotBlank(message = "Course code cannot be blank")
    private String courseCode;

    @Builder.Default
    private Boolean active = true;

    private String coverImage;

    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
    private LocalDate startDate;

    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
    private LocalDate endDate;

    @AssertTrue(message = "End date must be after start date")
    private boolean isValidDateRange() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
