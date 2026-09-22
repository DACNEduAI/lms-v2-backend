package edu.ai.lms.core.quiz3.app.port.in.usecase;

import edu.ai.lms.core.quiz3.adapter.in.http.response.GetQuestionLearningOutcomesResponse;

import java.util.UUID;

public interface GetQuestionLearningOutcomesUseCase {

    GetQuestionLearningOutcomesResponse getLearningOutcomes(UUID quizId, UUID questionId);
}
