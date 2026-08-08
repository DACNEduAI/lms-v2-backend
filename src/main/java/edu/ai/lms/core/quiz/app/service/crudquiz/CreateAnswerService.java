package edu.ai.lms.core.quiz.app.service.crudquiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;

import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateAnswerResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateAnswerCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateAnswerUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.service.quizworkflow.QuizAccessPolicy;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAnswerService implements CreateAnswerUseCase {

    private final SaveAnswerPort saveAnswerPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateAnswerResponse createAnswer(CreateAnswerCommand command) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Question question = loadQuestionPort.getByIdWithoutAnswers(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + command.questionId()));

        Quiz quiz = loadQuizPort.getById(question.getQuizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found"));

        verifyQuizOwnership(user, quiz);
        QuizAccessPolicy.requireMutable(quiz);

        Answer answer = command.answer();
        answer.setQuestionId(question.getId());

        Answer savedAnswer = saveAnswerPort.save(answer);

        return CreateAnswerResponse.builder()
                .answerNumber(savedAnswer.getAnswerNumber())
                .content(savedAnswer.getContent())
                .correct(savedAnswer.isCorrect())
                .explanation(savedAnswer.getExplanation())
                .questionId(savedAnswer.getQuestionId())
                .createdAt(savedAnswer.getCreatedAt())
                .build();
    }

    private void verifyQuizOwnership(CurrentActor user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));
        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to add answers to this quiz's questions");
        }
    }

}
