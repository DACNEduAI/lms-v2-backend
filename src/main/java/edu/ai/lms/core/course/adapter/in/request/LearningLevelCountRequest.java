package edu.ai.lms.core.course.adapter.in.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
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
public class LearningLevelCountRequest {
    private UUID customLearningLevelId;
    private String customLearningLevelClientTempId;

    @Min(value = 0, message = "questionCount must be >= 0")
    private int questionCount;
}
