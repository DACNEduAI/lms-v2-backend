package edu.ai.lms.core.quiz.app.port.in.usecase;

import edu.ai.lms.core.quiz.adapter.in.http.response.QuizGenerationJobResponse;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;

public interface GenerateQuizUseCase {
    QuizGenerationJobResponse generateQuiz(GenerateQuizCommand command);
}
