package edu.ai.lms.core.quiz.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateQuestionService implements CreateQuestionUseCase {

    private final SaveQuestionPort saveQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateQuestionResponse createQuestion(CreateQuestionCommand command) {
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.findById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, quiz);

        Question question = command.question();
        question.setQuizId(quiz.getId());

        Question savedQuestion = saveQuestionPort.save(question);

        return CreateQuestionResponse.builder()
                .questionId(savedQuestion.getId())
                .content(savedQuestion.getContent())
                .type(savedQuestion.getType() != null ? savedQuestion.getType().name() : null)
                .points(savedQuestion.getPoints())
                .quizId(savedQuestion.getQuizId())
                .createdAt(savedQuestion.getCreatedAt())
                .build();
    }

    private void verifyQuizOwnership(User user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to add questions to this quiz");
        }
    }
}
