package edu.ai.lms.core.user.api;

import edu.ai.lms.common.api.Roles;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        Roles role
) {
}
