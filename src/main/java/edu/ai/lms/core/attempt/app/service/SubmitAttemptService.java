package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.SubmitAttemptResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.exception.AttemptNotFoundException;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.exception.QuizNotFoundForAttemptException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;
import edu.ai.lms.core.attempt.app.port.in.usecase.SubmitAttemptUseCase;
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
public class SubmitAttemptService implements SubmitAttemptUseCase {

    private static final long SUBMISSION_GRACE_SECONDS = 5L;

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final LoadAttemptPort loadAttemptPort;
    private final SaveAttemptPort saveAttemptPort;
    private final AttemptScoringService attemptScoringService;
    private final AttemptAutoFinalizeService attemptAutoFinalizeService;

    @Override
    @Transactional
    public SubmitAttemptResponse submitAttempt(SubmitAttemptCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));

        if (user.getRole() != Roles.STUDENT) {
            throw new IllegalArgumentException("Only students can submit attempts");
        }

        if (!loadQuizAttemptPolicyPort.isStudentInCourse(user.getId(), command.courseId())) {
            throw new IllegalArgumentException("Student is not enrolled in this course");
        }

        QuizAttemptPolicy policy = loadQuizAttemptPolicyPort.getQuizAttemptPolicy(command.courseId(), command.quizId())
                .orElseThrow(() -> new QuizNotFoundForAttemptException("Quiz not found in course"));

        LocalDateTime now = LocalDateTime.now();
        attemptAutoFinalizeService.finalizeExpiredStartedAttemptIfNeeded(user.getId(), command.quizId(), now);

        Attempt startedAttempt = loadAttemptPort.getLatestStartedAttempt(user.getId(), command.quizId())
                .orElseThrow(() -> new AttemptNotFoundException("No active started attempt found for this quiz"));

        if (isLate(startedAttempt.getDueTime(), now)) {
            return saveLateSubmission(startedAttempt, policy, command.timeSpentSeconds(), now);
        }

        double score = attemptScoringService.calculateScore(
                loadQuizAttemptPolicyPort.getQuestionAnswerKeys(command.quizId()),
                command.submittedAnswers()
        );

        startedAttempt.setSubmittedTime(now);
        startedAttempt.setTimeSpentSeconds(command.timeSpentSeconds());
        startedAttempt.setScore(score);
        startedAttempt.setStatus(Attempt.AttemptStatus.FINISHED);
        startedAttempt.setErrorReason(null);

        Attempt saved = saveAttemptPort.save(startedAttempt);
        return toResponse(saved);
    }

    private SubmitAttemptResponse saveLateSubmission(
            Attempt startedAttempt,
            QuizAttemptPolicy policy,
            Integer timeSpentSeconds,
            LocalDateTime now
    ) {
        startedAttempt.setSubmittedTime(now);
        startedAttempt.setTimeSpentSeconds(timeSpentSeconds);

        if (policy.hasTimer()) {
            startedAttempt.setStatus(Attempt.AttemptStatus.ERROR);
            startedAttempt.setScore(null);
            startedAttempt.setErrorReason("Submission exceeded due time + 5 seconds");
        } else {
            startedAttempt.setStatus(Attempt.AttemptStatus.FINISHED);
            startedAttempt.setScore(0.0);
            startedAttempt.setErrorReason("Submission exceeded due time + 5 seconds");
        }

        Attempt saved = saveAttemptPort.save(startedAttempt);
        return toResponse(saved);
    }

    private boolean isLate(LocalDateTime dueTime, LocalDateTime now) {
        if (dueTime == null) {
            throw new InvalidAttemptSubmissionException("Attempt due time is missing");
        }
        return now.isAfter(dueTime.plusSeconds(SUBMISSION_GRACE_SECONDS));
    }

    private SubmitAttemptResponse toResponse(Attempt attempt) {
        return SubmitAttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuizId())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .timeSpentSeconds(attempt.getTimeSpentSeconds())
                .submittedTime(attempt.getSubmittedTime())
                .dueTime(attempt.getDueTime())
                .errorReason(attempt.getErrorReason())
                .build();
    }
}
