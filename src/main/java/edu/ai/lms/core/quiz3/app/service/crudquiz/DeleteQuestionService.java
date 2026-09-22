package edu.ai.lms.core.quiz3.app.service.crudquiz;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.adapter.in.http.response.DeleteQuestionResponse;
import edu.ai.lms.core.quiz3.app.domain.Question;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz3.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz3.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz3.app.port.in.usecase.DeleteQuestionUseCase;
import edu.ai.lms.core.quiz3.app.port.out.DeleteQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;
import edu.ai.lms.core.quiz3.app.service.quizworkflow.QuizAccessPolicy;

@Service
@RequiredArgsConstructor
public class DeleteQuestionService implements DeleteQuestionUseCase {

    private final DeleteQuestionPort deleteQuestionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public DeleteQuestionResponse deleteQuestion(UUID quizId, UUID questionId) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + quizId));

        verifyQuizOwnership(user, quiz);
        QuizAccessPolicy.requireMutable(quiz);

        Question question = loadQuestionPort.getByIdWithoutAnswers(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + questionId));

        if (!question.getQuizId().equals(quizId)) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        deleteQuestionPort.deleteById(questionId);

        return DeleteQuestionResponse.builder()
                .questionId(questionId)
                .message("Question deleted successfully")
                .build();
    }

    private void verifyQuizOwnership(CurrentActor user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to delete questions in this quiz");
        }
    }
}
