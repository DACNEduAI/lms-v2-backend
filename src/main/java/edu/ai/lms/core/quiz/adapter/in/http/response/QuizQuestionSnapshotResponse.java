package edu.ai.lms.core.quiz.adapter.in.http.response;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz.app.domain.Question;

public record QuizQuestionSnapshotResponse(
        UUID questionId,
        String content,
        Question.QuestionType questionType,
        String sourceType,
        UUID sourceBankQuestionId,
        UUID sourceQuestionId,
        long version,
        List<AnswerResponse> answers) {

    public static QuizQuestionSnapshotResponse from(Question question) {
        return new QuizQuestionSnapshotResponse(
                question.getId(), question.getContent(), question.getType(),
                question.getSourceType().name(),
                question.getSourceBankQuestionId(), question.getSourceQuestionId(), question.getVersion(),
                question.getAnswers().stream()
                        .map(answer -> new AnswerResponse(answer.getAnswerNumber(), answer.getContent(),
                                answer.isCorrect(), answer.getExplanation()))
                        .toList());
    }

    public record AnswerResponse(Long answerNumber, String content, boolean correct, String explanation) {
    }
}
