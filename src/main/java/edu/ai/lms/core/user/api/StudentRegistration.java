package edu.ai.lms.core.user.api;

public record StudentRegistration(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String avatarUrl,
        String studentId,
        Long enrollmentYear
) {
}
