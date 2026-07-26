package edu.ai.lms.core.quiz.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteAnswerResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.AnswerNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteAnswerUseCase;
import edu.ai.lms.core.quiz.app.port.out.DeleteAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteAnswerService implements DeleteAnswerUseCase {

    private final DeleteAnswerPort deleteAnswerPort;
    private final LoadAnswerPort loadAnswerPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public DeleteAnswerResponse deleteAnswer(UUID questionId, Long answerNumber) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Question question = loadQuestionPort.getByIdWithoutAnswers(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + questionId));

        Quiz quiz = loadQuizPort.getById(question.getQuizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found"));

        verifyQuizOwnership(user, quiz);

        Answer answer = loadAnswerPort.getByKey(questionId, answerNumber)
                .orElseThrow(() -> new AnswerNotFoundException("Answer not found"));

        deleteAnswerPort.deleteByKey(questionId, answerNumber);

        return DeleteAnswerResponse.builder()
                .answerNumber(answerNumber)
                .questionId(questionId)
                .message("Answer deleted successfully")
                .build();
    }

    private void verifyQuizOwnership(CurrentActor user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to delete answers in this quiz");
        }
    }
}
