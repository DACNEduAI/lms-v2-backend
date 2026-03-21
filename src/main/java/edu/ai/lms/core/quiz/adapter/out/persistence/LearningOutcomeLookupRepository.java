package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface LearningOutcomeLookupRepository {

    List<UUID> findExistingLearningOutcomeIdsByCourseIdAndIds(
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);

    List<UUID> findExistingGroupLearningOutcomeIdsByCourseIdAndIds(
            @Param("courseId") UUID courseId,
            @Param("groupLearningOutcomeIds") List<UUID> groupLearningOutcomeIds);

    List<UUID> findLearningOutcomeIdsByCourseIdAndGroupIds(
            @Param("courseId") UUID courseId,
            @Param("groupLearningOutcomeIds") List<UUID> groupLearningOutcomeIds);
}
