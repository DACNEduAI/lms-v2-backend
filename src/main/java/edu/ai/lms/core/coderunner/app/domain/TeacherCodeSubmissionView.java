package edu.ai.lms.core.coderunner.app.domain;

import java.time.Instant;
import java.util.UUID;

public record TeacherCodeSubmissionView(
        UUID submissionId,
        UUID attemptId,
        UUID studentId,
        String studentEmail,
        String status,
        String verdict,
        double score,
        double maxScore,
        int attemptCount,
        Instant acceptedAt,
        Instant completedAt,
        boolean selectedForGrading) { }
