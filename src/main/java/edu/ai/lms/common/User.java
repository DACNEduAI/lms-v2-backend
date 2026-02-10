package edu.ai.lms.common;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User implements UserDetails {
    private String password;
    private String email;
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

    @Override
    public String getPassword() {
        return this.password;
    }
}
