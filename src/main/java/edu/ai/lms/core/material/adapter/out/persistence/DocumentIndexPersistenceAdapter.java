package edu.ai.lms.core.material.adapter.out.persistence;

import edu.ai.lms.core.material.app.domain.DocumentIndex;
import edu.ai.lms.core.material.app.port.out.persistence.DocumentIndexPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class DocumentIndexPersistenceAdapter implements DocumentIndexPort {
    private final DocumentIndexRepository repository;

    @Override
    public Optional<DocumentIndex> findReady(String bucketName, String fileKey, String indexProfile) {
        return repository.findReady(bucketName, fileKey, indexProfile);
    }

    @Override
    public DocumentIndex save(DocumentIndex index) {
        repository.save(index);
        return index;
    }

    @Override
    public void deleteByDocument(String bucketName, String fileKey) {
        repository.deleteByDocument(bucketName, fileKey);
    }
}
