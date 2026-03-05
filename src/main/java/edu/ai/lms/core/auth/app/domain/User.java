package edu.ai.lms.core.auth.app.domain;

import edu.ai.lms.common.Roles;
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
public class User implements UserDetails {
    private UUID id;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Roles role;

    /**
     * Converts the current role enum to a String.
     */
    public String getRoleAsString() {
        return this.role != null ? this.role.name() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRoleAsString()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }
}
