package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;

/**
 * Package-private MyBatis mapper for the exam_matrices and exam_matrix_rows tables.
 */
@Mapper
interface ExamMatrixRepository {

    Optional<ExamMatrix> findById(@Param("id") UUID id);

    List<ExamMatrix> findAllByCourseId(@Param("courseId") UUID courseId);

    List<ExamMatrixRow> findRowsByMatrixId(@Param("matrixId") UUID matrixId);

    int insertMatrix(ExamMatrix matrix);

    int updateMatrix(ExamMatrix matrix);

    int insertRow(ExamMatrixRow row);

    void deleteRowsByMatrixId(@Param("matrixId") UUID matrixId);

    void deleteById(@Param("id") UUID id);
}
