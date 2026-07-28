package edu.ai.lms.core.user.api;

import edu.ai.lms.common.api.Roles;

import java.util.UUID;

/**
 * Immutable credential view published only for the authentication module.
 */
public record LoginAccountView(
        UUID id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String avatarUrl,
        Roles role,
        String studentId,
        Long enrollmentYear,
        String employeeId,
        String phoneNumber,
        String department,
        String specialization
) {
}
