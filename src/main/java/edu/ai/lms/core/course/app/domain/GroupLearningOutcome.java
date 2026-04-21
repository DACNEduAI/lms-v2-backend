package edu.ai.lms.core.course.app.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
public class GroupLearningOutcome {

    private UUID id;

    private String name;
    private String description;

    private UUID courseId;

    @Builder.Default
    private List<LearningOutcome> learningOutcomes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
