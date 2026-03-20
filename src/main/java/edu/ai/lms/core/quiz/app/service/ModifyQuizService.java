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

        // Update quiz fields
        Quiz updatedQuiz = command.quiz();
        updatedQuiz.setId(existingQuiz.getId());
        updatedQuiz.setCourseId(existingQuiz.getCourseId());
        updatedQuiz.setTeacherId(existingQuiz.getTeacherId());
        updatedQuiz.setStudentId(existingQuiz.getStudentId());

        Quiz savedQuiz = saveQuizPort.save(updatedQuiz);

        return ModifyQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .quizTitle(savedQuiz.getQuizTitle())
                .timeLimitMinutes(savedQuiz.getTimeLimitMinutes())
                .passScore(savedQuiz.getPassScore())
                .courseId(savedQuiz.getCourseId())
                .active(savedQuiz.getActive())
                .updatedAt(savedQuiz.getUpdatedAt())
                .build();
    }

    private void verifyQuizOwnership(UserAuth user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to modify this quiz");
        }
    }
}
