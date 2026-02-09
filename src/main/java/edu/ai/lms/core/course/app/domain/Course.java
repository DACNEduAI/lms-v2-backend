package edu.ai.lms.core.course.app.domain;

import jakarta.validation.constraints.AssertTrue;
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
public class Course {

    private UUID id;

    private String courseName;

    private String description;

    private String courseCode;

    private Teacher teacher;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @AssertTrue(message = "End date must be after start date")
    private boolean isValidDateRange() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }

    @Builder.Default
    private Boolean active = true;

    private String coverImage;

    @Builder.Default
    private List<Chapter> chapters = new ArrayList<>();

    @Builder.Default
    private List<LearningOutcome> learningOutcomes = new ArrayList<>();
}
