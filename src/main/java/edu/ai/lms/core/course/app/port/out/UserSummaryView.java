package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.common.api.Roles;

import java.util.UUID;

public record UserSummaryView(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        Roles role
) {
}
