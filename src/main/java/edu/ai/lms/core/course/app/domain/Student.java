package edu.ai.lms.core.course.app.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student {

    private Long enrollmentYear;

    @Builder.Default
    private List<LearningOutcome> masteredLearningOutcomes = new ArrayList<>();
}
