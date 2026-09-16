package edu.ai.lms.core.learnermodel.adapter.in.http;

import edu.ai.lms.core.learnermodel.adapter.in.http.mapper.LearnerModelResponseMapper;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.AllLearnerModelPredictionsResponse;
import edu.ai.lms.core.learnermodel.adapter.in.http.response.SingleLearnerModelPredictionResponse;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.PredictLearnerModelUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/{courseId}/learner-model")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Learner Model", description = "Course-scoped knowledge-tracing predictions for the authenticated student.")
@RequiredArgsConstructor
public class LearnerModelController {
    private final PredictLearnerModelUseCase predictionUseCase;
    private final LearnerModelResponseMapper responseMapper;

    @PostMapping("/dkt-custom/learning-outcomes/{learningOutcomeId}/predict")
    @Operation(summary = "Predict one learning outcome with DKT-Custom")
    public ResponseEntity<SingleLearnerModelPredictionResponse> predictDktCustom(
            @PathVariable UUID courseId, @PathVariable UUID learningOutcomeId) {
        return predictSingle(courseId, learningOutcomeId, LearnerModelType.DKT_CUSTOM);
    }

    @PostMapping("/dkt-custom/learning-outcomes/predict")
    @Operation(summary = "Predict every course learning outcome with DKT-Custom")
    public ResponseEntity<AllLearnerModelPredictionsResponse> predictAllDktCustom(@PathVariable UUID courseId) {
        return predictAll(courseId, LearnerModelType.DKT_CUSTOM);
    }

    @PostMapping("/dkt-pykt/learning-outcomes/{learningOutcomeId}/predict")
    @Operation(summary = "Predict one learning outcome with DKT-pyKT")
    public ResponseEntity<SingleLearnerModelPredictionResponse> predictDktPykt(
            @PathVariable UUID courseId, @PathVariable UUID learningOutcomeId) {
        return predictSingle(courseId, learningOutcomeId, LearnerModelType.DKT_PYKT);
    }

    @PostMapping("/dkt-pykt/learning-outcomes/predict")
    @Operation(summary = "Predict every course learning outcome with DKT-pyKT")
    public ResponseEntity<AllLearnerModelPredictionsResponse> predictAllDktPykt(@PathVariable UUID courseId) {
        return predictAll(courseId, LearnerModelType.DKT_PYKT);
    }

    @PostMapping("/akt-custom/learning-outcomes/{learningOutcomeId}/predict")
    @Operation(summary = "Predict one learning outcome with AKT-Custom")
    public ResponseEntity<SingleLearnerModelPredictionResponse> predictAktCustom(
            @PathVariable UUID courseId, @PathVariable UUID learningOutcomeId) {
        return predictSingle(courseId, learningOutcomeId, LearnerModelType.AKT_CUSTOM);
    }

    @PostMapping("/akt-custom/learning-outcomes/predict")
    @Operation(summary = "Predict every course learning outcome with AKT-Custom")
    public ResponseEntity<AllLearnerModelPredictionsResponse> predictAllAktCustom(@PathVariable UUID courseId) {
        return predictAll(courseId, LearnerModelType.AKT_CUSTOM);
    }

    @PostMapping("/akt-pykt/learning-outcomes/{learningOutcomeId}/predict")
    @Operation(summary = "Predict one learning outcome with AKT-pyKT")
    public ResponseEntity<SingleLearnerModelPredictionResponse> predictAktPykt(
            @PathVariable UUID courseId, @PathVariable UUID learningOutcomeId) {
        return predictSingle(courseId, learningOutcomeId, LearnerModelType.AKT_PYKT);
    }

    @PostMapping("/akt-pykt/learning-outcomes/predict")
    @Operation(summary = "Predict every course learning outcome with AKT-pyKT")
    public ResponseEntity<AllLearnerModelPredictionsResponse> predictAllAktPykt(@PathVariable UUID courseId) {
        return predictAll(courseId, LearnerModelType.AKT_PYKT);
    }

    private ResponseEntity<SingleLearnerModelPredictionResponse> predictSingle(
            UUID courseId, UUID learningOutcomeId, LearnerModelType model) {
        var result = predictionUseCase.predictSingle(
                new PredictLearnerModelCommand(courseId, learningOutcomeId, model));
        return ResponseEntity.ok(responseMapper.toSingle(result));
    }

    private ResponseEntity<AllLearnerModelPredictionsResponse> predictAll(
            UUID courseId, LearnerModelType model) {
        var result = predictionUseCase.predictAll(
                new PredictLearnerModelCommand(courseId, null, model));
        return ResponseEntity.ok(responseMapper.toAll(result));
    }
}
