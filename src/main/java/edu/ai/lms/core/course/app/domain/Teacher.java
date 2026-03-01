package edu.ai.lms.core.course.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Teacher extends User {

    private String employeeId;

    private String phoneNumber;

    private String department;

    private String specialization;

    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    {
        this.setRole(Roles.TEACHER);
    }
}