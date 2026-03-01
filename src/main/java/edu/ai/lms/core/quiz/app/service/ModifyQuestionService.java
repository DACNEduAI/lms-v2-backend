package edu.ai.lms.core.quiz.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyQuestionService implements ModifyQuestionUseCase {

    private final SaveQuestionPort saveQuestionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyQuestionResponse modifyQuestion(ModifyQuestionCommand command) {
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.findById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, quiz);

        Question existingQuestion = loadQuestionPort.findByIdWithoutAnswers(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + command.questionId()));

        if (!existingQuestion.getQuizId().equals(command.quizId())) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        Question updatedQuestion = command.question();
        updatedQuestion.setId(existingQuestion.getId());
        updatedQuestion.setQuizId(existingQuestion.getQuizId());

        Question savedQuestion = saveQuestionPort.save(updatedQuestion);

        return ModifyQuestionResponse.builder()
                .questionId(savedQuestion.getId())
                .content(savedQuestion.getContent())
                .type(savedQuestion.getType() != null ? savedQuestion.getType().name() : null)
                .points(savedQuestion.getPoints())
                .quizId(savedQuestion.getQuizId())
                .updatedAt(savedQuestion.getUpdatedAt())
                .build();
    }

    private void verifyQuizOwnership(User user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to modify questions in this quiz");
        }
    }
}
