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

        if (user.getRole() == Roles.TEACHER) {
            quiz.setTeacherId(user.getId());
        } else {
            quiz.setStudentId(user.getId());
        }

        // Save quiz
        Quiz savedQuiz = saveQuizPort.save(quiz);

        return CreateQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .quizTitle(savedQuiz.getQuizTitle())
                .timeLimitMinutes(savedQuiz.getTimeLimitMinutes())
                .passScore(savedQuiz.getPassScore())
                .courseId(savedQuiz.getCourseId())
                .active(savedQuiz.getActive())
                .createdAt(savedQuiz.getCreatedAt())
                .build();
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
