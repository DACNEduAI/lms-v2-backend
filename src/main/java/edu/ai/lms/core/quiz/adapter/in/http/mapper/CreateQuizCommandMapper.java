package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.CreateAnswerRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuestionRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuizRequest;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;

@Component
public class CreateQuizCommandMapper {

    public CreateQuizCommand toCommand(UUID courseId, CreateQuizRequest request) {
        List<Question> questions = new ArrayList<>();

        if (request.getQuestions() != null) {
            questions = request.getQuestions().stream()
                    .map(this::mapQuestion)
                    .collect(Collectors.toList());
        }

        Quiz quiz = Quiz.builder()
                .quizTitle(request.getQuizTitle())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .questions(questions)
                .build();

        return CreateQuizCommand.builder()
                .courseId(courseId)
                .quiz(quiz)
                .build();
    }

    private Question mapQuestion(CreateQuestionRequest req) {
        List<Answer> answers = new ArrayList<>();

        if (req.getAnswers() != null) {
            answers = req.getAnswers().stream()
                    .map(this::mapAnswer)
                    .collect(Collectors.toList());
        }

        return Question.builder()
                .content(req.getContent())
                .type(req.getType() != null ? Question.QuestionType.valueOf(req.getType()) : null)
                .points(req.getPoints())
                .answers(answers)
                .build();
    }

    private Answer mapAnswer(CreateAnswerRequest req) {
        return Answer.builder()
                .content(req.getContent())
                .isCorrect(req.isCorrect())
                .explanation(req.getExplanation())
                .build();
    }
}
