package edu.ai.lms.core.material.adapter.out.persistence;

import edu.ai.lms.core.material.app.domain.DocumentIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
interface DocumentIndexRepository {
    Optional<DocumentIndex> findReady(
            @Param("bucketName") String bucketName,
            @Param("fileKey") String fileKey,
            @Param("indexProfile") String indexProfile);

    void save(DocumentIndex index);

    void deleteByDocument(@Param("bucketName") String bucketName, @Param("fileKey") String fileKey);
}
