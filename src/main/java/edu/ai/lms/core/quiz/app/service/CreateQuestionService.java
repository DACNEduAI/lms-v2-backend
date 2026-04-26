package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.common.event.SystemActivityEvent;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CreateQuestionResponse createQuestion(CreateQuestionCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, quiz);

        Question question = command.question();
        question.setQuizId(quiz.getId());

        Question savedQuestion = saveQuestionPort.save(question);
        List<CreateQuestionResponse.AnswerSummary> answerSummaries = new ArrayList<>();

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

        // Publish activity event
        eventPublisher.publishEvent(SystemActivityEvent.builder()
                .userId(user.getId())
                .actionType("CREATE")
                .entityType("QUESTION")
                .entityId(savedQuestion.getId())
                .entityName("Câu hỏi trong " + quiz.getQuizTitle())
                .description("Giảng viên vừa thêm câu hỏi mới vào bài kiểm tra: " + quiz.getQuizTitle())
                .timestamp(LocalDateTime.now())
                .build());

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
