package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calculates the attempt score using equal-weight scoring.
 * <p>
 * Each gradable question (MULTIPLE_CHOICE / TRUE_FALSE) has the same weight.
 * The total score is scaled to {@code DEFAULT_MAX_SCORE} (10 points by default).
 * ESSAY is excluded; Code Runner contributes the ratio from a completed SUBMIT bound to this attempt.
 */
@Service
@RequiredArgsConstructor
public class AttemptScoringService {

    private final AttemptCodeRunnerPort codeRunnerPort;

    /** The maximum possible score for any quiz attempt. */
    public static final double DEFAULT_MAX_SCORE = 10.0;

    public record ScoringResult(double score, double codeRunnerScore, boolean hasPendingSubmissions) {}

    public ScoringResult calculateScore(List<QuestionAnswerKey> answerKeys, List<SubmittedAnswer> submittedAnswers,
                                        UUID attemptId, UUID studentId) {
        if (answerKeys == null || answerKeys.isEmpty()) {
            return new ScoringResult(0.0, 0.0, false);
        }
        List<SubmittedAnswer> answers = submittedAnswers == null ? List.of() : submittedAnswers;

        // Build lookup maps from the answer key
        Map<UUID, QuestionAnswerKey.QuestionType> questionTypes = answerKeys.stream()
                .collect(Collectors.toMap(
                        QuestionAnswerKey::getQuestionId,
                        QuestionAnswerKey::getQuestionType,
                        (left, right) -> left
                ));

        Map<UUID, Set<Long>> correctAnswersByQuestion = answerKeys.stream()
                .filter(key -> key.getCorrectAnswerNumber() != null)
                .collect(Collectors.groupingBy(
                        QuestionAnswerKey::getQuestionId,
                        Collectors.mapping(QuestionAnswerKey::getCorrectAnswerNumber, Collectors.toSet())
                ));

        Map<UUID, SubmittedAnswer> submittedByQuestion = answers.stream()
                .collect(Collectors.toMap(
                        SubmittedAnswer::getQuestionId,
                        answer -> answer,
                        (left, right) -> left
                ));

        // Count only question types supported by the current automatic scorer.
        long totalGradable = answerKeys.stream()
                .map(QuestionAnswerKey::getQuestionId)
                .distinct()
                .filter(qId -> {
                    QuestionAnswerKey.QuestionType t = questionTypes.get(qId);
                    return isAutomaticallyGradable(t);
                })
                .count();

        if (totalGradable == 0) {
            return new ScoringResult(0.0, 0.0, false);
        }

        double earnedRatios = 0;
        double codeRunnerEarnedRatios = 0;
        boolean hasPendingSubmissions = false;
        for (UUID questionId : questionTypes.keySet()) {
            QuestionAnswerKey.QuestionType type = questionTypes.get(questionId);
            if (!isAutomaticallyGradable(type)) continue;
            SubmittedAnswer answer = submittedByQuestion.get(questionId);
            if (answer == null) continue;
            if (type == QuestionAnswerKey.QuestionType.CODE_RUNNER) {
                if (answer.getCodeSubmissionId() == null) continue;
                if (!codeRunnerPort.isValidSubmit(answer.getCodeSubmissionId(), attemptId, studentId, questionId)) {
                    throw new InvalidAttemptSubmissionException("Code submission does not belong to this attempt");
                }
                var ratio = codeRunnerPort.findCompletedSubmitRatio(
                        answer.getCodeSubmissionId(), attemptId, studentId, questionId);
                if (ratio.isEmpty()) {
                    hasPendingSubmissions = true;
                    continue;
                }
                earnedRatios += ratio.getAsDouble();
                codeRunnerEarnedRatios += ratio.getAsDouble();
            } else {
                Set<Long> correctAnswers = correctAnswersByQuestion.getOrDefault(questionId, Collections.emptySet());
                if (answer.getStudentAnswerId() != null && correctAnswers.contains(answer.getStudentAnswerId())) {
                    earnedRatios += 1;
                }
            }
        }

        // Scale to DEFAULT_MAX_SCORE
        double rawRatio = earnedRatios / totalGradable;
        double score = Math.round(rawRatio * DEFAULT_MAX_SCORE * 100.0) / 100.0;
        double codeRunnerScore = Math.round((codeRunnerEarnedRatios / totalGradable)
                * DEFAULT_MAX_SCORE * 100.0) / 100.0;
        return new ScoringResult(score, codeRunnerScore, hasPendingSubmissions);
    }

    private boolean isAutomaticallyGradable(QuestionAnswerKey.QuestionType type) {
        return type == QuestionAnswerKey.QuestionType.MULTIPLE_CHOICE
                || type == QuestionAnswerKey.QuestionType.TRUE_FALSE
                || type == QuestionAnswerKey.QuestionType.CODE_RUNNER;
    }

    OptionalDouble findCompletedCodeRunnerRatio(
            UUID submissionId,
            UUID attemptId,
            UUID studentId,
            UUID questionId) {
        return codeRunnerPort.findCompletedSubmitRatio(submissionId, attemptId, studentId, questionId);
    }
}
