package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.QuestionGradeResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper
interface AttemptQuestionResponseRepository {

    void deleteByAttemptId(@Param("attemptId") UUID attemptId);

    void insert(
            @Param("attemptId") UUID attemptId,
            @Param("result") QuestionGradeResult result,
            @Param("finalizedAt") LocalDateTime finalizedAt
    );
}
