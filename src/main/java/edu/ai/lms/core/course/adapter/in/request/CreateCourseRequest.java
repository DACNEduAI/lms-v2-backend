package edu.ai.lms.core.course.adapter.in.request;

import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import edu.ai.lms.core.course.app.domain.Chapter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // Use @SuperBuilder if there is inheritance
public class CreateCourseRequest {
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

    @Builder.Default
    private List<CreateLearningOutcomeRequest> learningOutcomes = new ArrayList<>();

}
