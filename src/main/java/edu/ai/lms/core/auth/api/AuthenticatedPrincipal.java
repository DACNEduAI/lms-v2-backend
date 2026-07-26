package edu.ai.lms.core.auth.api;

import edu.ai.lms.common.api.Roles;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable authentication result shared at the auth module boundary.
 */
public record AuthenticatedPrincipal(
        UUID userId,
        String email,
        Roles role,
        Set<String> authorities
) {
    public AuthenticatedPrincipal {
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }
}
