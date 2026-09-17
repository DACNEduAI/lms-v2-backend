package edu.ai.lms.core.learnermodel.adapter.out.persistence;

import edu.ai.lms.core.learnermodel.app.domain.LearnerState;
import edu.ai.lms.core.learnermodel.app.port.out.LearnerStatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LearnerStatePersistenceAdapter implements LearnerStatePort {
    private final LearnerStateRepository repository;

    @Override
    public boolean existsForAttempt(UUID attemptId) {
        return repository.existsForAttempt(attemptId);
    }

    @Override
    public boolean saveIfAbsent(LearnerState state) {
        if (repository.insertState(state) == 0) {
            return false;
        }
        if (!state.outcomes().isEmpty()) {
            repository.insertOutcomes(state.id(), state.outcomes());
        }
        return true;
    }

    @Override
    public List<LearnerState> findByStudentAndCourse(UUID studentId, UUID courseId) {
        return repository.findByStudentAndCourse(studentId, courseId).stream()
                .map(row -> new LearnerState(row.id(), row.studentId(), row.courseId(), row.attemptId(),
                        row.calculatedAt(), repository.findOutcomes(row.id())))
                .toList();
    }

    @Override
    public Optional<LearnerState> findLatestByStudentAndCourse(UUID studentId, UUID courseId) {
        return repository.findLatestByStudentAndCourse(studentId, courseId)
                .map(row -> new LearnerState(row.id(), row.studentId(), row.courseId(), row.attemptId(),
                        row.calculatedAt(), repository.findOutcomes(row.id())));
    }
}
