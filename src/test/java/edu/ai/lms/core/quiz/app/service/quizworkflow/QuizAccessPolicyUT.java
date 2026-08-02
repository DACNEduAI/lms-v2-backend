package edu.ai.lms.core.quiz.app.service.quizworkflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz.app.exception.QuizLifecycleConflictException;

class QuizAccessPolicyUT {
    @Test
    void draftAndReviewPendingRemainMutable() {
        assertDoesNotThrow(() -> QuizAccessPolicy.requireMutable(
                Quiz.builder().lifecycleStatus(QuizLifecycleStatus.DRAFT).build()));
        assertDoesNotThrow(() -> QuizAccessPolicy.requireMutable(
                Quiz.builder().lifecycleStatus(QuizLifecycleStatus.REVIEW_PENDING).build()));
    }

    @Test
    void readyAndPublishedAreImmutable() {
        assertThrows(QuizLifecycleConflictException.class, () -> QuizAccessPolicy.requireMutable(
                Quiz.builder().lifecycleStatus(QuizLifecycleStatus.READY_TO_PUBLISH).build()));
        assertThrows(QuizLifecycleConflictException.class, () -> QuizAccessPolicy.requireMutable(
                Quiz.builder().lifecycleStatus(QuizLifecycleStatus.PUBLISHED).build()));
    }
}
