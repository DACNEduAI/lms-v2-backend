package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.core.quiz.app.domain.PendingQuizTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
interface QuizDeadlineRepository {
    List<PendingQuizTarget> findStudentsWithApproachingDeadlines(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);
}
