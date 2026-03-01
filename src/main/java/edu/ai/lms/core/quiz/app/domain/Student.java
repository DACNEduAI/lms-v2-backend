package edu.ai.lms.core.quiz.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student extends User {

    private Long enrollmentYear;

    {
        this.setRole(Roles.STUDENT);
    }
}
