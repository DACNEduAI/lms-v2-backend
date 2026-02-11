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

    private UUID id;

    private String title;
    private String description;
    private Integer orderIndex;

    private Course course;

    @Builder.Default
    private List<Material> materials = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}