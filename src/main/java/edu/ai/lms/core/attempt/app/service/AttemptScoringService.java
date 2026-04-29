package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calculates the attempt score using equal-weight scoring.
 * <p>
 * Each gradable question (MULTIPLE_CHOICE / TRUE_FALSE) has the same weight.
 * The total score is scaled to {@code DEFAULT_MAX_SCORE} (10 points by default).
 * ESSAY questions are excluded from automatic scoring.
 */
@Service
public class AttemptScoringService {

    /** The maximum possible score for any quiz attempt. */
    public static final double DEFAULT_MAX_SCORE = 10.0;

    public double calculateScore(List<QuestionAnswerKey> answerKeys, List<SubmittedAnswer> submittedAnswers) {
        if (answerKeys == null || answerKeys.isEmpty() || submittedAnswers == null || submittedAnswers.isEmpty()) {
            return 0.0;
        }

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

        Map<UUID, Long> submittedByQuestion = submittedAnswers.stream()
                .collect(Collectors.toMap(
                        SubmittedAnswer::getQuestionId,
                        SubmittedAnswer::getStudentAnswerId,
                        (left, right) -> left
                ));

        // Count total gradable questions (non-ESSAY from the full key set)
        long totalGradable = answerKeys.stream()
                .map(QuestionAnswerKey::getQuestionId)
                .distinct()
                .filter(qId -> {
                    QuestionAnswerKey.QuestionType t = questionTypes.get(qId);
                    return t != null && t != QuestionAnswerKey.QuestionType.ESSAY;
                })
                .count();

        if (totalGradable == 0) {
            return 0.0;
        }

        // Count correctly answered questions
        long correct = 0;
        for (Map.Entry<UUID, Long> entry : submittedByQuestion.entrySet()) {
            UUID questionId = entry.getKey();
            QuestionAnswerKey.QuestionType questionType = questionTypes.get(questionId);

            if (questionType == null || questionType == QuestionAnswerKey.QuestionType.ESSAY) {
                continue;
            }

            Set<Long> correctAnswers = correctAnswersByQuestion.getOrDefault(questionId, Collections.emptySet());
            if (correctAnswers.contains(entry.getValue())) {
                correct++;
            }
        }

        // Scale to DEFAULT_MAX_SCORE
        double rawRatio = (double) correct / totalGradable;
        double score = Math.round(rawRatio * DEFAULT_MAX_SCORE * 100.0) / 100.0;
        return score;
    }
}
