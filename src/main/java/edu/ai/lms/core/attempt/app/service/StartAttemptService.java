package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.StartAttemptResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.exception.AttemptLimitExceededException;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.exception.QuizNotFoundForAttemptException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.in.command.StartAttemptCommand;
import edu.ai.lms.core.attempt.app.port.in.usecase.StartAttemptUseCase;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StartAttemptService implements StartAttemptUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final LoadAttemptPort loadAttemptPort;
    private final SaveAttemptPort saveAttemptPort;
    private final AttemptAutoFinalizeService attemptAutoFinalizeService;

    @Override
    @Transactional
    public StartAttemptResponse startAttempt(StartAttemptCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));

        if (user.getRole() != Roles.STUDENT) {
            throw new IllegalArgumentException("Only students can start attempts");
        }

        if (!loadQuizAttemptPolicyPort.isStudentInCourse(user.getId(), command.courseId())) {
            throw new IllegalArgumentException("Student is not enrolled in this course");
        }

        QuizAttemptPolicy policy = loadQuizAttemptPolicyPort.getQuizAttemptPolicy(command.courseId(), command.quizId())
                .orElseThrow(() -> new QuizNotFoundForAttemptException("Quiz not found in course"));

        LocalDateTime now = LocalDateTime.now();
        attemptAutoFinalizeService.finalizeExpiredStartedAttemptIfNeeded(user.getId(), command.quizId(), now);

        if (loadAttemptPort.getLatestStartedAttempt(user.getId(), command.quizId()).isPresent()) {
            throw new InvalidAttemptSubmissionException("There is already an active started attempt for this quiz");
        }

        long finishedAttempts = loadAttemptPort.countFinishedAttempts(user.getId(), command.quizId());
        if (policy.getAttemptLimited() != null && finishedAttempts >= policy.getAttemptLimited()) {
            throw new AttemptLimitExceededException("You have reached the attempt limit for this quiz");
        }

        LocalDateTime dueTime = resolveDueTime(policy, now);

        Attempt attempt = Attempt.builder()
                .studentId(user.getId())
                .quizId(command.quizId())
                .startTime(now)
                .dueTime(dueTime)
                .status(Attempt.AttemptStatus.STARTED)
                .build();

        Attempt saved = saveAttemptPort.save(attempt);

        return StartAttemptResponse.builder()
                .attemptId(saved.getId())
                .quizId(saved.getQuizId())
                .status(saved.getStatus().name())
                .startTime(saved.getStartTime())
                .dueTime(saved.getDueTime())
                .build();
    }

    private LocalDateTime resolveDueTime(QuizAttemptPolicy policy, LocalDateTime now) {
        if (policy.hasTimer()) {
            return now.plusMinutes(policy.getTimeLimitMinutes());
        }
        if (policy.getQuizDueTime() == null) {
            throw new InvalidAttemptSubmissionException("Quiz due time is missing for non-timed quiz");
        }
        return policy.getQuizDueTime();
    }
}
