package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.out.persistence.mapper.QuizJdbcMapper;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import edu.ai.lms.database.QuizJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizPersistenceAdapter implements SaveQuizPort, LoadQuizPort, DeleteQuizPort {

    private final QuizRepository quizRepository;
    private final QuizJdbcMapper quizJdbcMapper;

    @Override
    public Quiz save(Quiz quiz) {
        QuizJdbcTable entity = quizJdbcMapper.mapToQuizJdbcEntity(quiz);
        QuizJdbcTable savedEntity = quizRepository.save(entity);
        return quizJdbcMapper.mapToQuiz(savedEntity);
    }

    @Override
    public Optional<Quiz> findById(UUID quizId) {
        return quizRepository.findById(quizId)
                .map(quizJdbcMapper::mapToQuiz);
    }

    @Override
    public List<Quiz> findAllByCourseId(UUID courseId) {
        return quizRepository.findAllByCourseId(courseId).stream()
                .map(quizJdbcMapper::mapToQuiz)
                .collect(Collectors.toList());
    }

    @Override
    public List<Quiz> findByCourseIdAndTeacherId(UUID courseId, UUID teacherId) {
        return quizRepository.findByCourseIdAndTeacherId(courseId, teacherId).stream()
                .map(quizJdbcMapper::mapToQuiz)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID quizId) {
        quizRepository.deleteById(quizId);
    }
}
