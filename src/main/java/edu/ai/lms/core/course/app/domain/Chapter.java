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

    // Chapter is a weak entity - its identity is (chapterNumber, courseId)
    private Long chapterNumber; // Auto-increment within the course context

    private String title;
    private String description;

    private UUID courseId; // Part of composite primary key

    @Builder.Default
    private List<Material> materials = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}