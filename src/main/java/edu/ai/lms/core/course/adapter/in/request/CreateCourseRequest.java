package edu.ai.lms.core.course.adapter.in.request;

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

    @NotNull(message = "Start date cannot be blank")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be blank")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate endDate;

    @AssertTrue(message = "End date must be after start date")
    private boolean isValidDateRange() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }

    @Builder.Default
    private List<CreateLearningOutcomeRequest> learningOutcomes = new ArrayList<>();

}
