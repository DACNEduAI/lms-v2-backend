package edu.ai.lms.core.learnermodel.app.service;

import edu.ai.lms.core.learnermodel.app.domain.LearnerModelPredictionResult;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelNotFoundException;
import edu.ai.lms.core.learnermodel.app.port.in.command.PredictLearnerModelCommand;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.PredictLearnerModelUseCase;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerActorPort;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PredictLearnerModelService implements PredictLearnerModelUseCase {
    private final LoadLearnerActorPort actorPort;
    private final LoadLearnerCoursePort coursePort;
    private final LearnerModelPredictionCalculator calculator;

    @Override
    public LearnerModelPredictionResult predictSingle(PredictLearnerModelCommand command) {
        UUID studentId = requireStudentAccess(command.courseId());
        LearningOutcomeTarget target = coursePort
                .findLearningOutcome(command.courseId(), command.learningOutcomeId())
                .orElseThrow(() -> new LearnerModelNotFoundException(
                        "Learning outcome does not belong to the course"));
        return calculator.predictSingle(studentId, command.courseId(), command.model(), target);
    }

    @Override
    public LearnerModelPredictionResult predictAll(PredictLearnerModelCommand command) {
        UUID studentId = requireStudentAccess(command.courseId());
        return calculator.predictAll(studentId, command.courseId(), command.model());
    }

    private UUID requireStudentAccess(UUID courseId) {
        UUID studentId = actorPort.requireStudentId();
        if (!coursePort.isStudentEnrolled(studentId, courseId)) {
            throw new LearnerModelAccessDeniedException("Student is not enrolled in this course");
        }
        return studentId;
    }
}
