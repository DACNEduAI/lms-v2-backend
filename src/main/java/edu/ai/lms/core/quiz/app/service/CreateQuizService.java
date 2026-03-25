package edu.ai.lms.core.quiz.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateQuizService implements CreateQuizUseCase {

    private final SaveQuizPort saveQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;

    @Override
    @Transactional
    public CreateQuizResponse createQuiz(CreateQuizCommand command) {
        // Verify user is authenticated
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        // Verify user belongs to the course
        verifyCourseMembership(user, command.courseId());

        // Set quiz properties
        Quiz quiz = command.quiz();
        quiz.setCourseId(command.courseId());
        if (quiz.getUnlimited() == null) {
            quiz.setUnlimited(false);
        }
        if (quiz.getDisplay() == null) {
            quiz.setDisplay(true);
        }

        if (user.getRole() == Roles.TEACHER) {
            quiz.setTeacherId(user.getId());
            validateQuizTimeConfiguration(quiz);

        } else {
            quiz.setStudentId(user.getId());
            quiz.setUnlimited(true);
            quiz.setStartTime(null);
            quiz.setDueTime(null);
            quiz.setAttemptLimited(null);
        }

        // Save quiz
        Quiz savedQuiz = saveQuizPort.save(quiz);

        return CreateQuizResponse.builder()
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
                .createdAt(savedQuiz.getCreatedAt())
                .build();
    }

    private void validateQuizTimeConfiguration(Quiz quiz) {
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

    private void verifyCourseMembership(UserAuth user, java.util.UUID courseId) {
        boolean isMember = user.getRole() == Roles.TEACHER
                ? loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)
                : loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId);

        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }
}
