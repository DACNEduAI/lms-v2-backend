package edu.ai.lms.core.learnermodel.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

record LearnerStateRow(
        UUID id,
        UUID studentId,
        UUID courseId,
        UUID attemptId,
        LocalDateTime calculatedAt
) {
}
