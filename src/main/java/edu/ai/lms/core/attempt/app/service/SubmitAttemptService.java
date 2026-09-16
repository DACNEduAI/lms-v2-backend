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
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
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
    private final AttemptQuestionResultPort attemptQuestionResultPort;

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
        saveQuestionResults(startedAttempt, answerKeys, command.submittedAnswers());

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

    private void saveQuestionResults(
            Attempt attempt,
            List<QuestionAnswerKey> answerKeys,
            List<SubmittedAnswer> submittedAnswers) {
        if (submittedAnswers == null || submittedAnswers.isEmpty()) {
            return;
        }

        Map<UUID, QuestionAnswerKey> metadata = new LinkedHashMap<>();
        answerKeys.forEach(key -> metadata.putIfAbsent(key.getQuestionId(), key));
        Map<UUID, Set<Long>> correctAnswers = answerKeys.stream()
                .filter(key -> key.getCorrectAnswerNumber() != null)
                .collect(Collectors.groupingBy(
                        QuestionAnswerKey::getQuestionId,
                        LinkedHashMap::new,
                        Collectors.mapping(QuestionAnswerKey::getCorrectAnswerNumber, Collectors.toSet())));
        Map<UUID, SubmittedAnswer> submitted = submittedAnswers.stream()
                .collect(Collectors.toMap(
                        SubmittedAnswer::getQuestionId,
                        answer -> answer,
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<AttemptQuestionResult> results = new java.util.ArrayList<>();
        int order = 0;
        for (QuestionAnswerKey key : metadata.values()) {
            SubmittedAnswer answer = submitted.get(key.getQuestionId());
            if (answer == null || key.getQuestionType() == QuestionAnswerKey.QuestionType.ESSAY) {
                order++;
                continue;
            }

            Boolean correct;
            if (key.getQuestionType() == QuestionAnswerKey.QuestionType.CODE_RUNNER) {
                if (answer.getCodeSubmissionId() == null) {
                    order++;
                    continue;
                }
                OptionalDouble ratio = attemptScoringService.findCompletedCodeRunnerRatio(
                        answer.getCodeSubmissionId(), attempt.getId(), attempt.getStudentId(), key.getQuestionId());
                correct = ratio.isPresent() ? ratio.getAsDouble() >= 1.0d : null;
            } else {
                if (answer.getStudentAnswerId() == null) {
                    order++;
                    continue;
                }
                correct = correctAnswers.getOrDefault(key.getQuestionId(), Set.of())
                        .contains(answer.getStudentAnswerId());
            }

            int mappingCount = key.getLearningOutcomes() == null ? 0 : key.getLearningOutcomes().size();
            QuestionAnswerKey.LearningOutcomeSnapshot outcome = mappingCount == 1
                    ? key.getLearningOutcomes().getFirst()
                    : null;
            results.add(AttemptQuestionResult.builder()
                    .attemptId(attempt.getId())
                    .questionId(key.getQuestionId())
                    .ktQuestionId(key.getKtQuestionId())
                    .interactionOrder(order)
                    .correct(correct)
                    .learningOutcomeId(outcome == null ? null : outcome.learningOutcomeId())
                    .ktLearningOutcomeId(outcome == null ? null : outcome.ktLearningOutcomeId())
                    .learningOutcomeMappingCount(mappingCount)
                    .build());
            order++;
        }
        attemptQuestionResultPort.upsertAll(results);
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
