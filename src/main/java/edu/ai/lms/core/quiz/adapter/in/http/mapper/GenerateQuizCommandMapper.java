package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.GenerateQuizRequest;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;

@Component
public class GenerateQuizCommandMapper {

    public GenerateQuizCommand toCommand(GenerateQuizRequest request) {
        return GenerateQuizCommand.builder()
                .courseId(request.getCourseId())
                .chapterId(request.getChapterId())
                .quizTitle(request.getQuizTitle())
                .difficulty(request.getDifficulty())
                .questionCount(request.getQuestionCount())
                .build();
    }
}
