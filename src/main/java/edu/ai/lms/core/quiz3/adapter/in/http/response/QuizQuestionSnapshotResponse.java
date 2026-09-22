package edu.ai.lms.core.quiz3.adapter.in.http.response;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.quiz3.app.domain.Question;

public record QuizQuestionSnapshotResponse(
        UUID questionId,
        String content,
        Question.QuestionType questionType,
        String sourceType,
        UUID sourceQuestionId,
        long version,
        List<AnswerResponse> answers) {

    public static QuizQuestionSnapshotResponse from(Question question) {
        return new QuizQuestionSnapshotResponse(
                question.getId(), question.getContent(), question.getType(),
                question.getSourceType().name(), question.getSourceQuestionId(), question.getVersion(),
                question.getAnswers().stream()
                        .map(answer -> new AnswerResponse(answer.getAnswerNumber(), answer.getContent(),
                                answer.isCorrect(), answer.getExplanation()))
                        .toList());
    }

    public record AnswerResponse(Long answerNumber, String content, boolean correct, String explanation) {
    }
}
