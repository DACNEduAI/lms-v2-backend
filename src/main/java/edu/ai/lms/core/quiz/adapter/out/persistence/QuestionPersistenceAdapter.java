package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.out.persistence.mapper.AnswerJdbcMapper;
import edu.ai.lms.core.quiz.adapter.out.persistence.mapper.QuestionJdbcMapper;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.database.AnswerJdbcTable;
import edu.ai.lms.database.QuestionJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuestionPersistenceAdapter implements SaveQuestionPort, LoadQuestionPort, DeleteQuestionPort {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionJdbcMapper questionJdbcMapper;
    private final AnswerJdbcMapper answerJdbcMapper;

    @Override
    public Question save(Question question) {
        QuestionJdbcTable entity = questionJdbcMapper.mapToQuestionJdbcEntity(question);
        QuestionJdbcTable savedEntity = questionRepository.save(entity);
        return questionJdbcMapper.mapToQuestionWithoutAnswers(savedEntity);
    }

    @Override
    public Optional<Question> findById(UUID questionId) {
        return questionRepository.findById(questionId)
                .map(this::loadQuestionWithAnswers);
    }

    @Override
    public Optional<Question> findByIdWithoutAnswers(UUID questionId) {
        return questionRepository.findById(questionId)
                .map(questionJdbcMapper::mapToQuestionWithoutAnswers);
    }

    @Override
    public List<Question> findByQuizId(UUID quizId) {
        return questionRepository.findByQuizId(quizId).stream()
                .map(questionJdbcMapper::mapToQuestionWithoutAnswers)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID questionId) {
        // First delete all answers for this question
        answerRepository.deleteByQuestionId(questionId);
        // Then delete the question itself
        questionRepository.deleteById(questionId);
    }

    /**
     * Loads a question with its answers (sub-aggregate).
     */
    private Question loadQuestionWithAnswers(QuestionJdbcTable entity) {
        Question question = questionJdbcMapper.mapToQuestionWithoutAnswers(entity);
        List<AnswerJdbcTable> answerEntities = answerRepository.findByQuestionId(entity.getId());
        List<Answer> answers = answerEntities.stream()
                .map(answerJdbcMapper::mapToAnswer)
                .collect(Collectors.toList());
        question.setAnswers(answers);
        return question;
    }
}
