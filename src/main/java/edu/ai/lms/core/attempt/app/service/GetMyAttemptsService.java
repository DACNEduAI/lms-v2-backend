package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.GetMyAttemptsResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.exception.QuizNotFoundForAttemptException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.in.usecase.GetMyAttemptsUseCase;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyAttemptsService implements GetMyAttemptsUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final LoadAttemptPort loadAttemptPort;
    private final AttemptAutoCloseService attemptAutoCloseService;

    @Override
    @Transactional
    public GetMyAttemptsResponse getMyAttempts(UUID courseId, UUID quizId) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));

        if (user.getRole() != Roles.STUDENT) {
            throw new IllegalArgumentException("Only students can view attempts");
        }

        if (!loadQuizAttemptPolicyPort.isStudentInCourse(user.getId(), courseId)) {
            throw new IllegalArgumentException("Student is not enrolled in this course");
        }

        loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)
                .orElseThrow(() -> new QuizNotFoundForAttemptException("Quiz not found in course"));

        attemptAutoCloseService.isAlreadyStartedAttemptExpiredValidation(user.getId(), courseId, quizId, LocalDateTime.now());

        List<GetMyAttemptsResponse.AttemptItem> attempts = loadAttemptPort.getByStudentAndQuiz(user.getId(), quizId)
                .stream()
                .map(this::toItem)
                .toList();

        return GetMyAttemptsResponse.builder()
                .quizId(quizId)
                .attempts(attempts)
                .build();
    }

    private GetMyAttemptsResponse.AttemptItem toItem(Attempt attempt) {
        return GetMyAttemptsResponse.AttemptItem.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .startTime(attempt.getStartTime())
                .dueTime(attempt.getDueTime())
                .submittedTime(attempt.getSubmittedTime())
                .timeSpentSeconds(attempt.getTimeSpentSeconds())
                .errorReason(attempt.getErrorReason())
                .build();
    }
}
