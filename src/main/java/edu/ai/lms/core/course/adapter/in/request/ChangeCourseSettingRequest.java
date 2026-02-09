package edu.ai.lms.core.course.adapter.in.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
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

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
