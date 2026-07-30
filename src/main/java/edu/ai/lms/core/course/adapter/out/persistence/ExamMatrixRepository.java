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
    List<UUID> findUncoveredLearningOutcomeIds(@Param("matrixId") UUID matrixId);
    List<ExamMatrixRowOutcomeLink> findOutcomeLinksByMatrixId(@Param("matrixId") UUID matrixId);
    List<ExamMatrixLevelCountRecord> findLevelCountsByMatrixId(@Param("matrixId") UUID matrixId);
    List<ExamMatrixLevelCountOutcomeLink> findLevelCountOutcomeLinksByMatrixId(@Param("matrixId") UUID matrixId);
    List<CustomLearningLevel> findCustomLearningLevelsByIds(@Param("ids") Collection<UUID> ids);
    List<CustomLearningLevel> findCustomLearningLevelsByMatrixId(@Param("matrixId") UUID matrixId);
    List<CustomLearningLevelOutcomeLink> findCustomLearningLevelOutcomeLinksByIds(
            @Param("ids") Collection<UUID> ids);
    int insertMatrix(ExamMatrix matrix);
    int insertRow(ExamMatrixRow row);
    int reconcileRowOutcomeCandidates(@Param("rowId") UUID rowId);
    int insertRowOutcomeCandidates(@Param("rowId") UUID rowId);
    int clearRowOutcomeSelections(@Param("rowId") UUID rowId);
    int selectRowOutcomes(@Param("rowId") UUID rowId, @Param("learningOutcomeIds") List<UUID> learningOutcomeIds);
    int insertLevelCount(@Param("rowId") UUID rowId, @Param("count") ExamMatrixLevelCount count);
    int updateRow(ExamMatrixRow row);
    int pinCcctProfile(@Param("id") UUID id, @Param("ccctProfileId") UUID ccctProfileId);
    int updateMetadata(@Param("id") UUID id, @Param("title") String title, @Param("description") String description);
    int touch(@Param("id") UUID id);
    int deleteRow(@Param("matrixId") UUID matrixId, @Param("rowId") UUID rowId);
    void deleteById(@Param("id") UUID id);
}
