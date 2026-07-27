package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.course.app.domain.CustomLearningLevel;
import edu.ai.lms.core.course.app.domain.CustomLearningLevelOutcomeLink;

@Mapper
interface CustomLearningLevelRepository {
    List<CustomLearningLevel> findAllByCourseId(@Param("courseId") UUID courseId);
    List<CustomLearningLevel> lockAllByCourseId(@Param("courseId") UUID courseId);
    Optional<CustomLearningLevel> findByCourseIdAndId(
            @Param("courseId") UUID courseId,
            @Param("id") UUID id);
    long countMatrixUsages(@Param("id") UUID id);
    long countIncompatibleMatrixUsages(
            @Param("id") UUID id,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);
    List<CustomLearningLevelOutcomeLink> findOutcomeLinksByCourseId(@Param("courseId") UUID courseId);
    int insert(CustomLearningLevel level);
    int stageUpdate(@Param("id") UUID id, @Param("temporaryValue") String temporaryValue);
    int update(CustomLearningLevel level);
    int deleteOutcomeMappings(@Param("id") UUID id);
    int insertOutcomeMapping(@Param("id") UUID id, @Param("learningOutcomeId") UUID learningOutcomeId);
    int delete(@Param("id") UUID id);
}
