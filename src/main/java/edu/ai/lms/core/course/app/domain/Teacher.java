package edu.ai.lms.core.course.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Teacher extends UserAuth {

    private String employeeId;

    private String phoneNumber;

    private String department;

    private String specialization;

//    @Builder.Default
//    private List<Course> courses = new ArrayList<>();

    {
        this.setRole(Roles.TEACHER);
    }
}