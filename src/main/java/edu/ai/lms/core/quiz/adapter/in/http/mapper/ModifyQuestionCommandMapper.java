package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.CreateAnswerRequest;
import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuestionRequest;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;

@Component
public class ModifyQuestionCommandMapper {

    public ModifyQuestionCommand toCommand(UUID quizId, ModifyQuestionRequest.ModifyQuestionItem request) {
        List<Answer> answers = new ArrayList<>();
        if (request.getAnswers() != null) {
            answers = request.getAnswers().stream()
                    .map(this::mapAnswer)
                    .collect(Collectors.toList());
        }

        Question question = Question.builder()
                .content(request.getContent())
                .type(request.getType() != null ? Question.QuestionType.valueOf(request.getType()) : null)
                .answers(answers)
                .build();

        return ModifyQuestionCommand.builder()
                .quizId(quizId)
                .questionId(request.getQuestionId())
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
