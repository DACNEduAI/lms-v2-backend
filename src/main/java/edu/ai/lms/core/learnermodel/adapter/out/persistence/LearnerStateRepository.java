package edu.ai.lms.core.learnermodel.adapter.out.persistence;

import edu.ai.lms.core.learnermodel.app.domain.LearnerState;
import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcome;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface LearnerStateRepository {
    boolean existsForAttempt(@Param("attemptId") UUID attemptId);

    int insertState(@Param("state") LearnerState state);

    void insertOutcomes(@Param("stateId") UUID stateId, @Param("outcomes") List<LearnerStateOutcome> outcomes);

    List<LearnerStateRow> findByStudentAndCourse(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    Optional<LearnerStateRow> findLatestByStudentAndCourse(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    List<LearnerStateOutcome> findOutcomes(@Param("stateId") UUID stateId);
}
