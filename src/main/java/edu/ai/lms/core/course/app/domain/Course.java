package edu.ai.lms.core.course.app.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    private UUID id;

    private String courseName;

    private String description;

    private String courseCode;

    private Teacher teacher;

    @Builder.Default
    private Boolean active = true;

    private String coverImage;

    @Builder.Default
    private List<Chapter> chapters = new ArrayList<>();

    @Builder.Default
    private List<LearningOutcome> learningOutcomes = new ArrayList<>();
}
