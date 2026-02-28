package edu.ai.lms.core.course.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.common.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student extends User {

    private UUID id;

    private Long enrollmentYear;

    {
        this.setRole(Roles.STUDENT);
    }

    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    @Builder.Default
    private List<LearningOutcome> masteredLearningOutcomes = new ArrayList<>();
}
