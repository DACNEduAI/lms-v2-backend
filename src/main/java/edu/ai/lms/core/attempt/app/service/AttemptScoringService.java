package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.core.attempt.app.domain.QuestionAnswerKey;
import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttemptScoringService {

    public double calculateScore(List<QuestionAnswerKey> answerKeys, List<SubmittedAnswer> submittedAnswers) {
        if (answerKeys == null || answerKeys.isEmpty() || submittedAnswers == null || submittedAnswers.isEmpty()) {
            return 0.0;
        }

        Map<UUID, QuestionAnswerKey.QuestionType> questionTypes = answerKeys.stream()
                .collect(Collectors.toMap(
                        QuestionAnswerKey::getQuestionId,
                        QuestionAnswerKey::getQuestionType,
                        (left, right) -> left
                ));

        Map<UUID, Double> questionPoints = answerKeys.stream()
                .collect(Collectors.toMap(
                        QuestionAnswerKey::getQuestionId,
                        QuestionAnswerKey::getPoints,
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

        double total = 0.0;
        for (Map.Entry<UUID, Long> entry : submittedByQuestion.entrySet()) {
            UUID questionId = entry.getKey();
            QuestionAnswerKey.QuestionType questionType = questionTypes.get(questionId);

            if (questionType == null || questionType == QuestionAnswerKey.QuestionType.ESSAY) {
                continue;
            }

            Set<Long> correctAnswers = correctAnswersByQuestion.getOrDefault(questionId, Collections.emptySet());
            if (correctAnswers.contains(entry.getValue())) {
                total += questionPoints.getOrDefault(questionId, 0.0);
            }
        }

        return total;
    }
}
