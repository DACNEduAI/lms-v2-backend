package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.GetAttemptQuizSessionResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import edu.ai.lms.core.attempt.app.exception.AttemptNotFoundException;
import edu.ai.lms.core.attempt.app.exception.AttemptAccessDeniedException;
import edu.ai.lms.core.attempt.app.exception.QuizNotFoundForAttemptException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.in.usecase.GetAttemptQuizSessionUseCase;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz3.app.port.in.usecase.GetQuizUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Returns the delivery-safe question snapshot only while an attempt is active. */
@Service
@RequiredArgsConstructor
public class GetAttemptQuizSessionService implements GetAttemptQuizSessionUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final LoadAttemptPort loadAttemptPort;
    private final AttemptAutoCloseService attemptAutoCloseService;
    private final GetQuizUseCase getQuizUseCase;

    @Override
    @Transactional
    public GetAttemptQuizSessionResponse getSession(UUID courseId, UUID quizId) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));
        if (user.getRole() != Roles.STUDENT) {
            throw new AttemptAccessDeniedException("Only students can view an attempt session");
        }
        if (!loadQuizAttemptPolicyPort.isStudentInCourse(user.getId(), courseId)) {
            throw new AttemptAccessDeniedException("Student is not enrolled in this course");
        }
        loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)
                .orElseThrow(() -> new QuizNotFoundForAttemptException("Quiz not found in course"));

        attemptAutoCloseService.isAlreadyStartedAttemptExpiredValidation(user.getId(), courseId, quizId, LocalDateTime.now());
        Attempt attempt = loadAttemptPort.getLatestStartedAttempt(user.getId(), quizId)
                .orElseThrow(() -> new AttemptNotFoundException("No active started attempt found for this quiz"));
        GetQuizResponse quiz = getQuizUseCase.getQuiz(courseId, quizId);

        return GetAttemptQuizSessionResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .description(quiz.getDescription())
                .startTime(attempt.getStartTime())
                .dueTime(attempt.getDueTime())
                .questions(quiz.getQuestions().stream().map(this::toQuestion).toList())
                .build();
    }

    private GetAttemptQuizSessionResponse.Question toQuestion(GetQuizResponse.QuestionSummary question) {
        return GetAttemptQuizSessionResponse.Question.builder()
                .questionId(question.getQuestionId())
                .content(question.getContent())
                .contentFormat(question.getContentFormat())
                .type(question.getType())
                .learningOutcomes((question.getLearningOutcomes() == null ? List.<GetQuizResponse.LearningOutcomeSummary>of() : question.getLearningOutcomes()).stream()
                        .map(learningOutcome -> GetAttemptQuizSessionResponse.LearningOutcome.builder()
                                .id(learningOutcome.getId())
                                .code(learningOutcome.getCode())
                                .build())
                        .toList())
                .answers(question.getAnswers().stream()
                        .map(answer -> GetAttemptQuizSessionResponse.Answer.builder()
                                .answerNumber(answer.getAnswerNumber())
                                .content(answer.getContent())
                                .contentFormat(answer.getContentFormat())
                                .build())
                        .toList())
                .build();
    }
}
