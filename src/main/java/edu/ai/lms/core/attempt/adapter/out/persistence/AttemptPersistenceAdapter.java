package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptPersistenceAdapter implements SaveAttemptPort, LoadAttemptPort, LoadQuizAttemptPolicyPort {

    private final AttemptRepository attemptRepository;
    private final QuizAttemptPolicyRepository quizAttemptPolicyRepository;

    @Override
    public Attempt save(Attempt attempt) {
        return attemptRepository.save(attempt);
    }

    @Override
    public long countFinishedAttempts(UUID studentId, UUID quizId) {
        return attemptRepository.countFinishedAttempts(studentId, quizId);
    }

    @Override
    public Optional<Attempt> getLatestStartedAttempt(UUID studentId, UUID quizId) {
        return attemptRepository.getLatestStartedAttempt(studentId, quizId);
    }

    @Override
    public Optional<Attempt> getByIdAndStudentId(UUID attemptId, UUID studentId) {
        return attemptRepository.getByIdAndStudentId(attemptId, studentId);
    }

    @Override
    public List<Attempt> getByStudentAndQuiz(UUID studentId, UUID quizId) {
        return attemptRepository.getByStudentAndQuiz(studentId, quizId);
    }

    @Override
    public List<Attempt> getExpiredStartedAttempts(LocalDateTime threshold) {
        return attemptRepository.getExpiredStartedAttempts(threshold);
    }

    @Override
    public Optional<QuizAttemptPolicy> getQuizAttemptPolicy(UUID courseId, UUID quizId) {
        if (courseId == null) {
            return quizAttemptPolicyRepository.getQuizAttemptPolicyByQuizId(quizId);
        }
        return quizAttemptPolicyRepository.getQuizAttemptPolicy(courseId, quizId);
    }

    @Override
    public List<QuestionAnswerKey> getQuestionAnswerKeys(UUID quizId) {
        return quizAttemptPolicyRepository.getQuestionAnswerKeys(quizId);
    }

    @Override
    public boolean isStudentInCourse(UUID studentId, UUID courseId) {
        return quizAttemptPolicyRepository.isStudentInCourse(studentId, courseId);
    }

    @Override
    public boolean quizHasTimer(UUID quizId) {
        return quizAttemptPolicyRepository.quizHasTimer(quizId);
    }
}
