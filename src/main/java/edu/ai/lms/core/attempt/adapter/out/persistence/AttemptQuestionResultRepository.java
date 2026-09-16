package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mapper
interface AttemptQuestionResultRepository {
    void upsertAll(@Param("results") List<AttemptQuestionResult> results);

    int updateCorrectness(
            @Param("attemptId") UUID attemptId,
            @Param("questionId") UUID questionId,
            @Param("correct") boolean correct);

    List<AttemptQuestionResult> findFinishedHistory(
            @Param("studentId") UUID studentId,
            @Param("quizIds") Collection<UUID> quizIds);

    List<AttemptQuestionResult> findAttemptRecords(
            @Param("studentId") UUID studentId,
            @Param("quizId") UUID quizId,
            @Param("attemptIds") Collection<UUID> attemptIds);
}
