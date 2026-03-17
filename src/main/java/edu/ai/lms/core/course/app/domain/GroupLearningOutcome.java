package edu.ai.lms.core.course.app.domain;

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
