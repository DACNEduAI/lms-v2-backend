package edu.ai.lms.core.course.app.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomLearningLevel {
    private UUID id;
    private UUID matrixId;
    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
