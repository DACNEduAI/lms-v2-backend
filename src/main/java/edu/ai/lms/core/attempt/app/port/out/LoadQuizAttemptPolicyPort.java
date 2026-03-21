package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadQuizAttemptPolicyPort {

    Optional<QuizAttemptPolicy> getQuizAttemptPolicy(UUID courseId, UUID quizId);

    List<QuestionAnswerKey> getQuestionAnswerKeys(UUID quizId);

    boolean isStudentInCourse(UUID studentId, UUID courseId);

    boolean quizHasTimer(UUID quizId);
}
