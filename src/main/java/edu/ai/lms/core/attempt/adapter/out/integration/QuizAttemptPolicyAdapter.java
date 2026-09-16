package edu.ai.lms.core.attempt.adapter.out.integration;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.quiz.api.AttemptPolicyApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class QuizAttemptPolicyAdapter implements LoadQuizAttemptPolicyPort {

    private final AttemptPolicyApi attemptPolicyApi;
    private final CourseAccessApi courseAccessApi;

    @Override
    public Optional<QuizAttemptPolicy> getQuizAttemptPolicy(UUID courseId, UUID quizId) {
        return attemptPolicyApi.findPolicy(quizId)
                .filter(policy -> courseId == null || courseId.equals(policy.courseId()))
                .map(policy -> QuizAttemptPolicy.builder()
                        .quizId(policy.quizId())
                        .quizReleaseId(policy.quizReleaseId())
                        .courseId(policy.courseId())
                        .unlimited(policy.unlimited())
                        .timeLimitMinutes(policy.timeLimitMinutes())
                        .quizStartTime(policy.startTime())
                        .quizDueTime(policy.dueTime())
                        .attemptLimited(policy.attemptLimit())
                        .build());
    }

    @Override
    public List<QuestionAnswerKey> getQuestionAnswerKeys(UUID quizId) {
        return attemptPolicyApi.findAnswerKeys(quizId).stream()
                .map(key -> QuestionAnswerKey.builder()
                        .questionId(key.questionId())
                        .ktQuestionId(key.ktQuestionId())
                        .questionType(QuestionAnswerKey.QuestionType.valueOf(key.questionType()))
                        .correctAnswerNumber(key.correctAnswerNumber())
                        .learningOutcomes(key.learningOutcomes().stream()
                                .map(outcome -> new QuestionAnswerKey.LearningOutcomeSnapshot(
                                        outcome.learningOutcomeId(), outcome.ktLearningOutcomeId()))
                                .toList())
                        .build())
                .toList();
    }

    @Override
    public boolean isStudentInCourse(UUID studentId, UUID courseId) {
        return courseAccessApi.isStudentInCourse(studentId, courseId);
    }

    @Override
    public boolean quizHasTimer(UUID quizId) {
        return attemptPolicyApi.findPolicy(quizId)
                .map(policy -> !policy.unlimited()
                        && policy.timeLimitMinutes() != null
                        && policy.timeLimitMinutes() > 0)
                .orElse(false);
    }
}
