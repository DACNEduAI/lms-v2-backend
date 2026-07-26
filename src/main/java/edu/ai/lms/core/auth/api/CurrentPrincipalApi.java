package edu.ai.lms.core.auth.api;

import java.util.Optional;
import java.util.UUID;

public interface CurrentPrincipalApi {

    Optional<AuthenticatedPrincipal> currentPrincipal();

    default UUID requireCurrentUserId() {
        return currentPrincipal()
                .map(AuthenticatedPrincipal::userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated"));
    }
}
