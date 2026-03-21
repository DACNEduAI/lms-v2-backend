package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.out.DeleteQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizPersistenceAdapter implements SaveQuizPort, LoadQuizPort, DeleteQuizPort {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Override
    public Quiz save(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    @Override
    public Optional<Quiz> getById(UUID quizId) {
        return quizRepository.getById(quizId);
    }

    @Override
    public Optional<Quiz> getByIdWithQuestionsAndAnswers(UUID courseId, UUID quizId) {
        return quizRepository.getByIdAndCourseId(quizId, courseId)
                .map(this::loadQuizWithQuestionsAndAnswers);
    }

    @Override
    public List<Quiz> getAllByCourseId(UUID courseId) {
        return quizRepository.getAllByCourseId(courseId).stream().collect(Collectors.toList());
    }

    @Override
    public List<Quiz> getTeacherCreatedByCourseId(UUID courseId) {
        return quizRepository.getTeacherCreatedByCourseId(courseId).stream().collect(Collectors.toList());
    }

    @Override
    public List<Quiz> getDisplayedTeacherCreatedByCourseId(UUID courseId) {
        return quizRepository.getDisplayedTeacherCreatedByCourseId(courseId).stream().collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID quizId) {
        quizRepository.deleteById(quizId);
    }

    private Quiz loadQuizWithQuestionsAndAnswers(Quiz quiz) {
        List<Question> questions = questionRepository.getByQuizId(quiz.getId()).stream()
                .map(this::loadQuestionWithAnswers)
                .collect(Collectors.toList());
        quiz.setQuestions(questions);
        return quiz;
    }

    private Question loadQuestionWithAnswers(Question question) {
        List<Answer> answers = answerRepository.getByQuestionId(question.getId()).stream()
                .collect(Collectors.toList());
        question.setAnswers(answers);
        return question;
    }
}
