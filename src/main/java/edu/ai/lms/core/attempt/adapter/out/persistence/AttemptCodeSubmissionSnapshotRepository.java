package edu.ai.lms.core.attempt.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
interface AttemptCodeSubmissionSnapshotRepository {

    void deleteByAttemptId(@Param("attemptId") UUID attemptId);

    void insert(@Param("attemptId") UUID attemptId,
                @Param("questionId") UUID questionId,
                @Param("submissionId") UUID submissionId);

    List<AttemptCodeSubmissionSnapshotRow> findByAttemptId(@Param("attemptId") UUID attemptId);
}
