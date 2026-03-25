package edu.ai.lms.core.quiz.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModifyQuizService implements ModifyQuizUseCase {

    private final SaveQuizPort saveQuizPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyQuizResponse modifyQuiz(ModifyQuizCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz existingQuiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify quiz belongs to the same course
        if (!existingQuiz.getCourseId().equals(command.courseId())) {
            throw new IllegalArgumentException("Quiz does not belong to this course");
        }

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, existingQuiz);

        Quiz requestedQuiz = command.quiz();
        Quiz updatedQuiz = mergeQuiz(existingQuiz, requestedQuiz);
        validateQuizTimeConfiguration(updatedQuiz);

        Quiz savedQuiz = saveQuizPort.save(updatedQuiz);

        return ModifyQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .quizTitle(savedQuiz.getQuizTitle())
            .unlimited(savedQuiz.getUnlimited())
                .timeLimitMinutes(savedQuiz.getTimeLimitMinutes())
                .passScore(savedQuiz.getPassScore())
                .startTime(savedQuiz.getStartTime())
                .dueTime(savedQuiz.getDueTime())
                .attemptLimited(savedQuiz.getAttemptLimited())
                .courseId(savedQuiz.getCourseId())
                .display(savedQuiz.getDisplay())
                .updatedAt(savedQuiz.getUpdatedAt())
                .build();
    }

    private Quiz mergeQuiz(Quiz existingQuiz, Quiz requestedQuiz) {
        Quiz merged = Quiz.builder()
                .id(existingQuiz.getId())
                .quizTitle(requestedQuiz.getQuizTitle() != null ? requestedQuiz.getQuizTitle() : existingQuiz.getQuizTitle())
                .unlimited(requestedQuiz.getUnlimited() != null ? requestedQuiz.getUnlimited() : existingQuiz.getUnlimited())
                .timeLimitMinutes(requestedQuiz.getTimeLimitMinutes() != null ? requestedQuiz.getTimeLimitMinutes() : existingQuiz.getTimeLimitMinutes())
                .passScore(requestedQuiz.getPassScore() != null ? requestedQuiz.getPassScore() : existingQuiz.getPassScore())
                .startTime(requestedQuiz.getStartTime() != null ? requestedQuiz.getStartTime() : existingQuiz.getStartTime())
                .dueTime(requestedQuiz.getDueTime() != null ? requestedQuiz.getDueTime() : existingQuiz.getDueTime())
                .attemptLimited(requestedQuiz.getAttemptLimited() != null ? requestedQuiz.getAttemptLimited() : existingQuiz.getAttemptLimited())
                .courseId(existingQuiz.getCourseId())
                .studentId(existingQuiz.getStudentId())
                .teacherId(existingQuiz.getTeacherId())
                .display(requestedQuiz.getDisplay() != null ? requestedQuiz.getDisplay() : existingQuiz.getDisplay())
                .createdAt(existingQuiz.getCreatedAt())
                .updatedAt(existingQuiz.getUpdatedAt())
                .build();

        return merged;
    }

    private void validateQuizTimeConfiguration(Quiz quiz) {
        if (quiz.getUnlimited() == null) {
            quiz.setUnlimited(false);
        }

        if (Boolean.TRUE.equals(quiz.getUnlimited())) {
            if (quiz.getStartTime() != null || quiz.getDueTime() != null) {
                throw new IllegalArgumentException("When unlimited is true, startTime and dueTime must be null");
            }
            quiz.setStartTime(null);
            quiz.setDueTime(null);
            return;
        }

        if (quiz.getDueTime() == null) {
            throw new IllegalArgumentException("dueTime is required when unlimited is false");
        }

        LocalDateTime startTime = quiz.getStartTime();
        LocalDateTime dueTime = quiz.getDueTime();
        if (startTime != null && dueTime.isBefore(startTime)) {
            throw new IllegalArgumentException("dueTime must be after or equal to startTime");
        }
    }

    private void verifyQuizOwnership(UserAuth user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to modify this quiz");
        }
    }
}
