package edu.ai.lms.core.course.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student extends UserAuth {

    private Long enrollmentYear;

    {
        this.setRole(Roles.STUDENT);
    }

//    @Builder.Default
//    private List<Course> courses = new ArrayList<>();

    @Builder.Default
    private List<LearningOutcome> masteredLearningOutcomes = new ArrayList<>();
}
