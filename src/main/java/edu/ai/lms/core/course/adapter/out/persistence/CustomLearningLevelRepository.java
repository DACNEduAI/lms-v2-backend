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
    List<CustomLearningLevel> findAllByMatrixId(@Param("matrixId") UUID matrixId);
    List<CustomLearningLevel> lockAllByMatrixId(@Param("matrixId") UUID matrixId);
    Optional<CustomLearningLevel> findByMatrixIdAndId(
            @Param("matrixId") UUID matrixId,
            @Param("id") UUID id);
    List<CustomLearningLevelOutcomeLink> findOutcomeLinksByMatrixId(@Param("matrixId") UUID matrixId);
    int insert(CustomLearningLevel level);
    int stageUpdate(@Param("id") UUID id, @Param("temporaryValue") String temporaryValue);
    int update(CustomLearningLevel level);
    int insertAllOutcomeMappings(@Param("id") UUID id, @Param("matrixId") UUID matrixId);
    int clearOutcomeSelections(@Param("id") UUID id);
    int selectOutcomeMappings(@Param("id") UUID id, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);
    int insertOutcomeMappingsForCourseMatrices(
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);
    int delete(@Param("id") UUID id);
}
