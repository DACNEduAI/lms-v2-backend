package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCustomLearningLevelRequest {
    @NotEmpty(message = "At least one learning outcome is required")
    private List<UUID> learningOutcomeIds = new ArrayList<>();

    @NotBlank(message = "Code cannot be blank")
    private String code;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;

    @Min(value = 0, message = "sortOrder must be >= 0")
    private int sortOrder;
}
