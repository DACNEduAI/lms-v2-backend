package edu.ai.lms.core.course.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.common.User;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Teacher extends User {

    private UUID id;

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