package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.Attempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
interface AttemptRepository {

    int insert(Attempt entity);

    int update(Attempt entity);

    int finalizeIfStillFinalizing(@Param("attemptId") UUID attemptId, @Param("score") Double score);

    default Attempt save(Attempt entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            insert(entity);
        } else {
            int updated = update(entity);
            if (updated == 0) {
                insert(entity);
            }
        }

        return getById(entity.getId()).orElseThrow();
    }

    Optional<Attempt> getById(@Param("attemptId") UUID attemptId);

    int closeIfStarted(
            @Param("attemptId") UUID attemptId,
            @Param("status") Attempt.AttemptStatus status,
            @Param("score") Double score,
            @Param("errorReason") String errorReason,
            @Param("submittedTime") LocalDateTime submittedTime
    );

    Optional<Attempt> getByIdAndStudentId(@Param("attemptId") UUID attemptId, @Param("studentId") UUID studentId);

    Optional<Attempt> getLatestStartedAttempt(@Param("studentId") UUID studentId, @Param("quizId") UUID quizId);

    long countFinishedAttempts(@Param("studentId") UUID studentId, @Param("quizId") UUID quizId);

    List<Attempt> getByStudentAndQuiz(@Param("studentId") UUID studentId, @Param("quizId") UUID quizId);
}
