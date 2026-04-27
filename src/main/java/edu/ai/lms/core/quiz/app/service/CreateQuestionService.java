package edu.ai.lms.core.quiz.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuestionResponse;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuestionUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateQuestionService implements CreateQuestionUseCase {

    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateQuestionResponse createQuestion(CreateQuestionCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, quiz);

        // set quizId in question (to know that which quiz this question belongs to)
        Question question = command.question();
        question.setQuizId(quiz.getId());

        // save question to database
        Question savedQuestion = saveQuestionPort.save(question);
        List<CreateQuestionResponse.AnswerSummary> answerSummaries = new ArrayList<>();

        // save the images to the cloud storage


        // return the question as a response
        if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
            answerSummaries = question.getAnswers().stream()
                    .map(answer -> {
                        answer.setQuestionId(savedQuestion.getId());
                        Answer savedAnswer = saveAnswerPort.save(answer);
                        return CreateQuestionResponse.AnswerSummary.builder()
                                .answerNumber(savedAnswer.getAnswerNumber())
                                .content(savedAnswer.getContent())
                                .isCorrect(savedAnswer.isCorrect())
                                .explanation(savedAnswer.getExplanation())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return CreateQuestionResponse.builder()
                .questionId(savedQuestion.getId())
                .content(savedQuestion.getContent())
                .type(savedQuestion.getType() != null ? savedQuestion.getType().name() : null)
                .points(savedQuestion.getPoints())
                .quizId(savedQuestion.getQuizId())
                .answers(answerSummaries)
                .createdAt(savedQuestion.getCreatedAt())
                .build();
    }

    private void verifyQuizOwnership(UserAuth user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to add questions to this quiz");
        }
    }
}
