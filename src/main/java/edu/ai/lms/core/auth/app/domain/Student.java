package edu.ai.lms.core.auth.app.domain;

import edu.ai.lms.common.Roles;
import edu.ai.lms.common.User;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student extends User {
    private UUID id;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Long enrollmentYear;

    {
        this.setRole(Roles.STUDENT);
    }
}