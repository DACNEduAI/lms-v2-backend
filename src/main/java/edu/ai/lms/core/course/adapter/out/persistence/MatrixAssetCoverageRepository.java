package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists LMS-only asset allocation metadata owned by Matrix custom learning levels. */
@Mapper
public interface MatrixAssetCoverageRepository {
    List<MatrixAssetCoverageRow> findByMatrixId(@Param("matrixId") UUID matrixId);
    int insert(MatrixAssetCoverageRow row);
}
