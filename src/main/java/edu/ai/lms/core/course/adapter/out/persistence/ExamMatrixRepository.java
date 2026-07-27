package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.course.app.domain.*;

@Mapper
interface ExamMatrixRepository {
    Optional<ExamMatrix> findById(@Param("id") UUID id);
    Optional<ExamMatrix> findByCourseIdAndId(@Param("courseId") UUID courseId, @Param("id") UUID id);
    Optional<ExamMatrix> lockByCourseIdAndId(@Param("courseId") UUID courseId, @Param("id") UUID id);
    List<ExamMatrixSummary> findAllSummariesByCourseId(@Param("courseId") UUID courseId);
    List<ExamMatrixRow> findRowsByMatrixId(@Param("matrixId") UUID matrixId);
    List<ExamMatrixRowOutcomeLink> findOutcomeLinksByMatrixId(@Param("matrixId") UUID matrixId);
    List<ExamMatrixLevelCountRecord> findLevelCountsByMatrixId(@Param("matrixId") UUID matrixId);
    List<ExamMatrixLevelCountOutcomeLink> findLevelCountOutcomeLinksByMatrixId(@Param("matrixId") UUID matrixId);
    List<CustomLearningLevel> findCustomLearningLevelsByIds(@Param("ids") Collection<UUID> ids);
    List<CustomLearningLevelOutcomeLink> findCustomLearningLevelOutcomeLinksByIds(
            @Param("ids") Collection<UUID> ids);
    int insertMatrix(ExamMatrix matrix);
    int insertRow(ExamMatrixRow row);
    int insertRowOutcome(@Param("rowId") UUID rowId, @Param("learningOutcomeId") UUID learningOutcomeId);
    int insertLevelCount(@Param("rowId") UUID rowId, @Param("count") ExamMatrixLevelCount count);
    int updateRow(ExamMatrixRow row);
    int updateMetadata(@Param("id") UUID id, @Param("title") String title, @Param("description") String description);
    int touch(@Param("id") UUID id);
    int deleteLevelCounts(@Param("rowId") UUID rowId);
    int deleteRowOutcomes(@Param("rowId") UUID rowId);
    int deleteRow(@Param("matrixId") UUID matrixId, @Param("rowId") UUID rowId);
    void deleteById(@Param("id") UUID id);
}
