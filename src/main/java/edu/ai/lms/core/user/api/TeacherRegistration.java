package edu.ai.lms.core.user.api;

public record TeacherRegistration(
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String avatarUrl,
        String employeeId,
        String phoneNumber,
        String department,
        String specialization
) {
}
