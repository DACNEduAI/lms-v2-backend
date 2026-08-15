package edu.ai.lms.core.material.app.port.out.persistence;

import edu.ai.lms.core.material.app.domain.DocumentIndex;

import java.util.Optional;

public interface DocumentIndexPort {
    Optional<DocumentIndex> findReady(String bucketName, String fileKey, String indexProfile);
    DocumentIndex save(DocumentIndex index);
    void deleteByDocument(String bucketName, String fileKey);
}
