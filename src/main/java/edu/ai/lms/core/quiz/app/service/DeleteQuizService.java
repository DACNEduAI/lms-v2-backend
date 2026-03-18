package edu.ai.lms.core.quiz.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.adapter.in.http.response.DeleteQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.DeleteQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteQuizService implements DeleteQuizUseCase {

    private final DeleteQuizPort deleteQuizPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public DeleteQuizResponse deleteQuiz(UUID courseId, UUID quizId) {
        User user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz existingQuiz = loadQuizPort.getById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + quizId));

        // Verify quiz belongs to the same course
        if (!existingQuiz.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Quiz does not belong to this course");
        }

        // Verify user is the creator of the quiz
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(existingQuiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(existingQuiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to delete this quiz");
        }

        deleteQuizPort.deleteById(quizId);

        return DeleteQuizResponse.builder()
                .quizId(quizId)
                .message("Quiz deleted successfully")
                .build();
    }
}
