package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.SubmitAttemptResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.attempt.app.exception.AttemptNotFoundException;
import edu.ai.lms.core.attempt.app.exception.QuizNotFoundForAttemptException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;
import edu.ai.lms.core.attempt.app.port.in.usecase.SubmitAttemptUseCase;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeSubmissionSnapshotPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.attempt.app.port.out.SaveAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.UnscheduleAttemptAutoClosePort;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmitAttemptService implements SubmitAttemptUseCase {

    private static final long SUBMISSION_GRACE_SECONDS = 5L;

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final LoadAttemptPort loadAttemptPort;
    private final SaveAttemptPort saveAttemptPort;
    private final UnscheduleAttemptAutoClosePort unscheduleAttemptAutoClosePort;
    private final AttemptScoringService attemptScoringService;
    private final AttemptCodeSubmissionSnapshotPort codeSubmissionSnapshotPort;
    private final AttemptAutoCloseService attemptAutoCloseService;

    @Override
    @Transactional
    public SubmitAttemptResponse submitAttempt(SubmitAttemptCommand command) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
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
        attemptAutoCloseService.isAlreadyStartedAttemptExpiredValidation(user.getId(), command.courseId(), command.quizId(), now);

        Attempt startedAttempt = loadAttemptPort.getLatestStartedAttempt(user.getId(), command.quizId())
                .orElseThrow(() -> new AttemptNotFoundException("No active started attempt found for this quiz"));

        if (isLate(startedAttempt.getDueTime(), now)) {
            return saveLateSubmission(startedAttempt, policy, command.timeSpentSeconds(), now);
        }

        List<edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey> answerKeys =
                loadQuizAttemptPolicyPort.getQuestionAnswerKeys(command.quizId());
        AttemptScoringService.ScoringResult scoringResult = attemptScoringService.calculateScore(
                answerKeys,
                command.submittedAnswers(),
                startedAttempt.getId(),
                user.getId()
        );
        codeSubmissionSnapshotPort.replace(startedAttempt.getId(),
                selectedCodeSubmissions(answerKeys, command.submittedAnswers()));

        startedAttempt.setSubmittedTime(now);
        startedAttempt.setTimeSpentSeconds(command.timeSpentSeconds());
        startedAttempt.setScore(scoringResult.hasPendingSubmissions()
                ? scoringResult.score() - scoringResult.codeRunnerScore()
                : scoringResult.score());
        
        if (scoringResult.hasPendingSubmissions()) {
            startedAttempt.setStatus(Attempt.AttemptStatus.FINALIZING);
        } else {
            startedAttempt.setStatus(Attempt.AttemptStatus.FINISHED);
        }
        startedAttempt.setErrorReason(null);

        Attempt saved = saveAttemptPort.save(startedAttempt);
        unscheduleAutoCloseBestEffort(saved.getId());
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
        unscheduleAutoCloseBestEffort(saved.getId());
        return toResponse(saved);
    }

    private void unscheduleAutoCloseBestEffort(UUID attemptId) {
        try {
            unscheduleAttemptAutoClosePort.unschedule(attemptId);
        } catch (RuntimeException ex) {
            log.warn("Failed to unschedule attempt auto-close job for attempt {}", attemptId, ex);
        }
    }

    private boolean isLate(LocalDateTime dueTime, LocalDateTime now) {
        if (dueTime == null) {
            // Unlimited mode: never treated as late.
            return false;
        }
        return now.isAfter(dueTime.plusSeconds(SUBMISSION_GRACE_SECONDS));
    }

    private Map<UUID, UUID> selectedCodeSubmissions(
            List<edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey> answerKeys,
            List<edu.ai.lms.core.attempt.app.domain.SubmittedAnswer> submittedAnswers) {
        Map<UUID, edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey.QuestionType> types = new LinkedHashMap<>();
        answerKeys.forEach(key -> types.putIfAbsent(key.getQuestionId(), key.getQuestionType()));
        Map<UUID, UUID> selected = new LinkedHashMap<>();
        if (submittedAnswers == null) return selected;
        submittedAnswers.forEach(answer -> {
            if (types.get(answer.getQuestionId()) == edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey.QuestionType.CODE_RUNNER
                    && answer.getCodeSubmissionId() != null) {
                selected.putIfAbsent(answer.getQuestionId(), answer.getCodeSubmissionId());
            }
        });
        return selected;
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
