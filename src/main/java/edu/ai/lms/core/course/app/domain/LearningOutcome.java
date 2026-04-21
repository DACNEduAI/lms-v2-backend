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
public class LearningOutcome {

    private UUID id;

    private String code;   // e.g. "CLO-1"
    private String description;

    private UUID courseId;

    private UUID groupLearningOutcomeId;

    private Course course;

    @Builder.Default
    private List<Student> students = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
