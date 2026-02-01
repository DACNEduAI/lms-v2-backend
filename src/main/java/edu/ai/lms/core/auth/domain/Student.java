package edu.ai.lms.core.auth.domain;

import lombok.*;
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
@Builder
public class Student implements UserDetails {
    private UUID id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Long enrollmentYear;
    private Roles role;

    /**
     * Converts the current role enum to a String.
     */
    public String getRoleAsString() {
        return this.role != null ? this.role.name() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Updated to use the actual role assigned to the user
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRoleAsString()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}