package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.CreateAnswerRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuestionRequest;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;

@Component
public class CreateQuestionCommandMapper {

    public CreateQuestionCommand toCommand(UUID quizId, CreateQuestionRequest.CreateQuestionItem request) {
        List<Answer> answers = new ArrayList<>();
        if (request.getAnswers() != null) {
            answers = request.getAnswers().stream()
                    .map(this::mapAnswer)
                    .collect(Collectors.toList());
        }

        Question question = Question.builder()
                .content(request.getContent())
                .type(request.getType() != null ? Question.QuestionType.valueOf(request.getType()) : null)
                .points(request.getPoints())
                .answers(answers)
                .build();

        return CreateQuestionCommand.builder()
                .quizId(quizId)
                .question(question)
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
