package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Quiz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadQuizPort {
    Optional<Quiz> getById(UUID quizId);
    Optional<Quiz> getByIdWithQuestionsAndAnswers(UUID courseId, UUID quizId);
    List<Quiz> getAllByCourseId(UUID courseId);
    List<Quiz> getTeacherCreatedByCourseId(UUID courseId);
    List<Quiz> getStudentCreatedByCourseIdAndStudentId(UUID courseId, UUID studentId);
}
