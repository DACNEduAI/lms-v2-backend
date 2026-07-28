package edu.ai.lms.core.auth.adapter.in.api;

import edu.ai.lms.core.auth.api.AuthenticatedPrincipal;
import edu.ai.lms.core.auth.api.CurrentPrincipalApi;
import edu.ai.lms.core.auth.api.TokenAuthenticationApi;
import edu.ai.lms.core.auth.adapter.out.security.SecurityUserPrincipal;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.adapter.out.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class AuthenticationApiAdapter implements CurrentPrincipalApi, TokenAuthenticationApi {

    private final JwtUtil jwtUtil;
    private final LoadStudentPort loadStudentPort;
    private final LoadTeacherPort loadTeacherPort;

    @Override
    public AuthenticatedPrincipal authenticate(String token) {
        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);
        UserAuth user = loadByEmailAndRole(email, role);
        if (!jwtUtil.validateToken(token, SecurityUserPrincipal.from(user))) {
            throw new IllegalArgumentException("Invalid token");
        }
        return toPrincipal(user);
    }

    @Override
    public Optional<AuthenticatedPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof SecurityUserPrincipal principal) {
            return Optional.of(new AuthenticatedPrincipal(
                    principal.id(),
                    principal.email(),
                    principal.role(),
                    principal.getAuthorities().stream()
                            .map(authority -> authority.getAuthority())
                            .collect(Collectors.toUnmodifiableSet())));
        }
        try {
            String email = authentication.getName();
            String role = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                    .findFirst()
                    .orElse(null);
            return Optional.of(toPrincipal(loadByEmailAndRole(email, role)));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private UserAuth loadByEmailAndRole(String email, String role) {
        if ("TEACHER".equals(role)) {
            return loadTeacherPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Teacher not found: " + email));
        }
        if ("STUDENT".equals(role)) {
            return loadStudentPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Student not found: " + email));
        }
        throw new UsernameNotFoundException("Unsupported user role");
    }

    private AuthenticatedPrincipal toPrincipal(UserAuth user) {
        return new AuthenticatedPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                java.util.Set.of("ROLE_" + user.getRoleAsString()));
    }
}
