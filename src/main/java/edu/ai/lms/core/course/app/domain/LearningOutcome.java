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
public class LearningOutcome {

    private UUID id;

    private String code;   // e.g. "CLO-1"
    private String description;

    private UUID courseId;

    private Course course;

    @Builder.Default
    private List<Student> students = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
