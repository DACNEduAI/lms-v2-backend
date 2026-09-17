package edu.ai.lms.core.learnermodel.adapter.in.http.mapper;

import edu.ai.lms.core.learnermodel.adapter.in.http.response.AllLearnerModelPredictionsResponse;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.LearningOutcomePredictionResponse;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.LearnerStateHistoryResponse;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.LearnerStateOutcomeResponse;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.LearnerStateResponse;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.SingleLearnerModelPredictionResponse;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelPredictionResult;
import org.springframework.stereotype.Component;

@Component
public class LearnerModelResponseMapper {
    public SingleLearnerModelPredictionResponse toSingle(LearnerModelPredictionResult result) {
        return new SingleLearnerModelPredictionResponse(
                result.courseId(),
                result.studentId(),
                result.model().wireName(),
                result.historyInteractionCount(),
                toPrediction(result.predictions().getFirst()));
    }

    public AllLearnerModelPredictionsResponse toAll(LearnerModelPredictionResult result) {
        return new AllLearnerModelPredictionsResponse(
                result.courseId(),
                result.studentId(),
                result.model().wireName(),
                result.historyInteractionCount(),
                result.predictions().stream().map(this::toPrediction).toList());
    }

    public LearnerStateHistoryResponse toStateHistory(
            java.util.UUID courseId,
            java.util.UUID studentId,
            java.util.List<edu.ai.lms.core.learnermodel.app.domain.LearnerState> states) {
        return new LearnerStateHistoryResponse(courseId, studentId, states.stream()
                .map(state -> new LearnerStateResponse(state.attemptId(), state.calculatedAt(), state.outcomes().stream()
                        .map(outcome -> new LearnerStateOutcomeResponse(
                                outcome.learningOutcomeId(), outcome.probability(), outcome.status()))
                        .toList()))
                .toList());
    }

    private LearningOutcomePredictionResponse toPrediction(
            edu.ai.lms.core.learnermodel.app.domain.LearningOutcomePrediction prediction) {
        return new LearningOutcomePredictionResponse(
                prediction.learningOutcomeId(),
                prediction.probability(),
                prediction.status(),
                prediction.basisQuestionIds());
    }
}
