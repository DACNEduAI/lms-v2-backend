package edu.ai.lms.core.learnermodel.adapter.out.integration;

import edu.ai.lms.core.course.api.CourseAccessApi;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerCoursePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LearnerCourseAdapter implements LoadLearnerCoursePort {
    private final CourseAccessApi courseAccessApi;
    private final LearningOutcomeCatalogApi learningOutcomeCatalogApi;

    @Override
    public boolean isStudentEnrolled(UUID studentId, UUID courseId) {
        return courseAccessApi.isStudentInCourse(studentId, courseId);
    }

    @Override
    public Optional<LearningOutcomeTarget> findLearningOutcome(UUID courseId, UUID learningOutcomeId) {
        return learningOutcomeCatalogApi.findByIds(courseId, List.of(learningOutcomeId)).stream()
                .findFirst()
                .map(outcome -> new LearningOutcomeTarget(
                        outcome.id(), outcome.ktLearningOutcomeId()));
    }

    @Override
    public List<LearningOutcomeTarget> findLearningOutcomes(UUID courseId) {
        return learningOutcomeCatalogApi.findByCourseId(courseId).stream()
                .map(outcome -> new LearningOutcomeTarget(
                        outcome.id(), outcome.ktLearningOutcomeId()))
                .toList();
    }
}
