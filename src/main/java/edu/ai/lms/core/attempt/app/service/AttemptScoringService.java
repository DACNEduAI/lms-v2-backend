package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
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

    public record ScoringResult(double score, Map<UUID, Boolean> correctnessByQuestion) {}

    public ScoringResult calculateScore(List<QuestionAnswerKey> answerKeys, List<SubmittedAnswer> submittedAnswers,
                                        UUID attemptId, UUID studentId) {
        if (answerKeys == null || answerKeys.isEmpty()) {
            return new ScoringResult(0.0, Map.of());
        }
        List<SubmittedAnswer> answers = submittedAnswers == null ? List.of() : submittedAnswers;

        // Build lookup maps from the answer key
        Map<UUID, QuestionAnswerKey> questions = answerKeys.stream()
                .collect(Collectors.toMap(QuestionAnswerKey::getQuestionId, key -> key,
                        (left, right) -> left, LinkedHashMap::new));
        questions.values().stream()
                .filter(question -> question.getQuestionType() == QuestionAnswerKey.QuestionType.MULTIPLE_CHOICE
                        || question.getQuestionType() == QuestionAnswerKey.QuestionType.TRUE_FALSE)
                .filter(question -> question.getAnswers().stream().filter(QuestionAnswerKey.AnswerSnapshot::correct).count() != 1)
                .findFirst()
                .ifPresent(question -> {
                    throw new InvalidAttemptSubmissionException(
                            "Published objective question must have exactly one correct answer");
                });
        Map<UUID, SubmittedAnswer> submittedByQuestion = new LinkedHashMap<>();
        for (SubmittedAnswer answer : answers) {
            QuestionAnswerKey question = questions.get(answer.getQuestionId());
            if (question == null) {
                throw new InvalidAttemptSubmissionException("Submitted question does not belong to this quiz");
            }
            if (submittedByQuestion.putIfAbsent(answer.getQuestionId(), answer) != null) {
                throw new InvalidAttemptSubmissionException("A question may only be submitted once");
            }
            if (question.getQuestionType() == QuestionAnswerKey.QuestionType.CODE_RUNNER
                    && answer.getStudentAnswerId() != null) {
                throw new InvalidAttemptSubmissionException("Code Runner questions do not accept answer numbers");
            }
            if (question.getQuestionType() != QuestionAnswerKey.QuestionType.CODE_RUNNER
                    && answer.getCodeSubmissionId() != null) {
                throw new InvalidAttemptSubmissionException("Objective questions do not accept code submissions");
            }
            if (question.getQuestionType() != QuestionAnswerKey.QuestionType.CODE_RUNNER
                    && answer.getStudentAnswerId() != null
                    && question.getAnswers().stream().noneMatch(option -> option.answerNumber() == answer.getStudentAnswerId())) {
                throw new InvalidAttemptSubmissionException("Submitted answer does not belong to its question");
            }
        }

        // Count only question types supported by the current automatic scorer.
        long totalGradable = questions.values().stream()
                .filter(key -> isAutomaticallyGradable(key.getQuestionType()))
                .count();

        if (totalGradable == 0) {
            return new ScoringResult(0.0, Map.of());
        }

        double earnedRatios = 0;
        Map<UUID, Boolean> correctness = new LinkedHashMap<>();
        for (QuestionAnswerKey question : questions.values()) {
            UUID questionId = question.getQuestionId();
            QuestionAnswerKey.QuestionType type = question.getQuestionType();
            if (!isAutomaticallyGradable(type)) continue;
            SubmittedAnswer answer = submittedByQuestion.get(questionId);
            boolean correct = false;
            if (answer == null) {
                correctness.put(questionId, false);
                continue;
            }
            if (type == QuestionAnswerKey.QuestionType.CODE_RUNNER) {
                if (answer.getCodeSubmissionId() == null) {
                    correctness.put(questionId, false);
                    continue;
                }
                if (!codeRunnerPort.isValidSubmit(answer.getCodeSubmissionId(), attemptId, studentId, questionId)) {
                    throw new InvalidAttemptSubmissionException("Code submission does not belong to this attempt");
                }
                var ratio = codeRunnerPort.findCompletedSubmitRatio(
                        answer.getCodeSubmissionId(), attemptId, studentId, questionId);
                if (ratio.isPresent()) {
                    earnedRatios += ratio.getAsDouble();
                    correct = ratio.getAsDouble() >= 1.0d;
                }
            } else {
                correct = answer.getStudentAnswerId() != null
                        && question.getAnswers().stream().anyMatch(option ->
                        option.answerNumber() == answer.getStudentAnswerId() && option.correct());
                if (correct) {
                    earnedRatios += 1;
                }
            }
            correctness.put(questionId, correct);
        }

        // Scale to DEFAULT_MAX_SCORE
        double rawRatio = earnedRatios / totalGradable;
        double score = Math.round(rawRatio * DEFAULT_MAX_SCORE * 100.0) / 100.0;
        return new ScoringResult(score, Map.copyOf(correctness));
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
