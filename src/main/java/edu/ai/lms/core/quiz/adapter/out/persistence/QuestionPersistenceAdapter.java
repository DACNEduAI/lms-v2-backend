package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuestionPersistenceAdapter implements SaveQuestionPort, LoadQuestionPort, DeleteQuestionPort {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Override
    public Question save(Question question) {
        Question savedQuestion = questionRepository.save(question);

        if (question.getImages() != null) {
            questionRepository.deleteImagesByQuestionId(savedQuestion.getId());

            question.getImages().forEach(image ->
                    questionRepository.insertImage(savedQuestion.getId(), image));
        }

        return questionRepository.getById(savedQuestion.getId())
                .map(this::loadQuestionWithAnswers)
                .orElseThrow();
    }

    @Override
    public Optional<Question> getById(UUID questionId) {
        return questionRepository.getById(questionId)
                .map(this::loadQuestionWithAnswers);
    }

    @Override
    public Optional<Question> getByIdWithoutAnswers(UUID questionId) {
        return questionRepository.getById(questionId);
    }

    @Override
    public List<Question> getByQuizId(UUID quizId) {
        return questionRepository.getByQuizId(quizId).stream().collect(Collectors.toList());
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
    private Question loadQuestionWithAnswers(Question question) {
        List<Answer> answers = answerRepository.getByQuestionId(question.getId()).stream().collect(Collectors.toList());
        question.setAnswers(answers);
        question.setImages(questionRepository.getImagesByQuestionId(question.getId()));
        return question;
    }
}
