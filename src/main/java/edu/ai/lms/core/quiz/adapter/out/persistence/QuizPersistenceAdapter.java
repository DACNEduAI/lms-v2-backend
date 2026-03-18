package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.out.persistence.mapper.AnswerJdbcMapper;
import edu.ai.lms.core.quiz.adapter.out.persistence.mapper.QuestionJdbcMapper;
import edu.ai.lms.core.quiz.adapter.out.persistence.mapper.QuizJdbcMapper;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import edu.ai.lms.database.AnswerJdbcTable;
import edu.ai.lms.database.QuestionJdbcTable;
import edu.ai.lms.database.QuizJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizPersistenceAdapter implements SaveQuizPort, LoadQuizPort, DeleteQuizPort {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuizJdbcMapper quizJdbcMapper;
    private final QuestionJdbcMapper questionJdbcMapper;
    private final AnswerJdbcMapper answerJdbcMapper;

    @Override
    public Quiz save(Quiz quiz) {
        QuizJdbcTable entity = quizJdbcMapper.mapToQuizJdbcEntity(quiz);
        QuizJdbcTable savedEntity = quizRepository.save(entity);
        return quizJdbcMapper.mapToQuiz(savedEntity);
    }

    @Override
    public Optional<Quiz> getById(UUID quizId) {
        return quizRepository.getById(quizId)
                .map(quizJdbcMapper::mapToQuiz);
    }

    @Override
    public Optional<Quiz> getByIdWithQuestionsAndAnswers(UUID courseId, UUID quizId) {
        return quizRepository.getByIdAndCourseId(quizId, courseId)
                .map(this::loadQuizWithQuestionsAndAnswers);
    }

    @Override
    public List<Quiz> getAllByCourseId(UUID courseId) {
        return quizRepository.getAllByCourseId(courseId).stream()
                .map(quizJdbcMapper::mapToQuiz)
                .collect(Collectors.toList());
    }

    @Override
    public List<Quiz> getByCourseIdAndTeacherId(UUID courseId, UUID teacherId) {
        return quizRepository.getByCourseIdAndTeacherId(courseId, teacherId).stream()
                .map(quizJdbcMapper::mapToQuiz)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID quizId) {
        quizRepository.deleteById(quizId);
    }

    private Quiz loadQuizWithQuestionsAndAnswers(QuizJdbcTable quizEntity) {
        List<Question> questions = questionRepository.getByQuizId(quizEntity.getId()).stream()
                .map(this::loadQuestionWithAnswers)
                .collect(Collectors.toList());
        return quizJdbcMapper.mapToQuizWithQuestionsAndAnswers(quizEntity, questions);
    }

    private Question loadQuestionWithAnswers(QuestionJdbcTable questionEntity) {
        Question question = questionJdbcMapper.mapToQuestionWithoutAnswers(questionEntity);
        List<Answer> answers = answerRepository.getByQuestionId(questionEntity.getId()).stream()
                .map(answerJdbcMapper::mapToAnswer)
                .collect(Collectors.toList());
        question.setAnswers(answers);
        return question;
    }
}
