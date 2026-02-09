package edu.ai.lms.core.course.adapter.in.request;

import edu.ai.lms.core.course.app.domain.Chapter;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
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

    @NotBlank(message = "Teacher ID cannot be blank")
    private UUID teacherId;

    @Builder.Default
    private Boolean active = true;

    private String coverImage;

    @NotBlank(message = "Start date cannot be blank")
    private LocalDateTime startDate;

    @NotBlank(message = "End date cannot be blank")
    private LocalDateTime endDate;

    @Builder.Default
    private List<Chapter> chapters = new ArrayList<>();

    @Builder.Default
    private List<CreateLearningOutcomeRequest> learningOutcomes = new ArrayList<>();

}
