package edu.ai.lms.core.auth.adapter.out.security;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security-specific representation. The auth domain remains framework-free.
 */
public record SecurityUserPrincipal(
        UUID id,
        String email,
        String password,
        edu.ai.lms.common.api.Roles role
) implements UserDetails {

    public static SecurityUserPrincipal from(UserAuth user) {
        return new SecurityUserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
