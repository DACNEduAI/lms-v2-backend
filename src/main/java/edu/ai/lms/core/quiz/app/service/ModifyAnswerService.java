package edu.ai.lms.core.quiz.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyAnswerResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.AnswerNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyAnswerCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyAnswerUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyAnswerService implements ModifyAnswerUseCase {

    private final SaveAnswerPort saveAnswerPort;
    private final LoadAnswerPort loadAnswerPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyAnswerResponse modifyAnswer(ModifyAnswerCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Question question = loadQuestionPort.getByIdWithoutAnswers(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + command.questionId()));

        Quiz quiz = loadQuizPort.getById(question.getQuizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found"));

        verifyQuizOwnership(user, quiz);

        Answer existingAnswer = loadAnswerPort.getByKey(command.questionId(), command.answerNumber())
                .orElseThrow(() -> new AnswerNotFoundException("Answer not found"));

        Answer updatedAnswer = command.answer();
        updatedAnswer.setAnswerNumber(existingAnswer.getAnswerNumber());
        updatedAnswer.setQuestionId(existingAnswer.getQuestionId());

        Answer savedAnswer = saveAnswerPort.save(updatedAnswer);

        return ModifyAnswerResponse.builder()
                .answerNumber(savedAnswer.getAnswerNumber())
                .content(savedAnswer.getContent())
                .correct(savedAnswer.isCorrect())
                .explanation(savedAnswer.getExplanation())
                .questionId(savedAnswer.getQuestionId())
                .updatedAt(savedAnswer.getUpdatedAt())
                .build();
    }

    private void verifyQuizOwnership(UserAuth user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to modify answers in this quiz");
        }
    }
}
