package edu.ai.lms.core.auth.app.domain;

import edu.ai.lms.common.Roles;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student extends UserAuth {
    private String studentId;
    private Long enrollmentYear;

    {
        this.setRole(Roles.STUDENT);
    }
}