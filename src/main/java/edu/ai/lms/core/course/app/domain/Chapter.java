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
public class Chapter {

    // Chapter is a weak entity - its identity is (chapterId, courseId)
    private UUID chapterId; // Auto-increment within the course context

    private String title;
    private String description;

    private UUID courseId; // Part of composite primary key

    @Builder.Default
    private List<UUID> learningOutcomeIds = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}