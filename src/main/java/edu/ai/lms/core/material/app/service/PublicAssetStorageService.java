package edu.ai.lms.core.material.app.service;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.material.adapter.out.storage.S3StorageAdapter;
import edu.ai.lms.core.material.api.PublicAssetStorageApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicAssetStorageService implements PublicAssetStorageApi {
    private final S3StorageAdapter storage;

    @Override
    public void upload(String bucketName, String storageKey, byte[] content, String mediaType) {
        storage.uploadFile(bucketName, storageKey, content, mediaType, true);
    }

    @Override
    public String publicUrl(String bucketName, String storageKey) {
        return storage.getPublicUrl(bucketName, storageKey);
    }

    @Override
    public byte[] read(String bucketName, String storageKey) {
        return storage.readFile(bucketName, storageKey);
    }

    @Override
    public void delete(String bucketName, String storageKey) {
        storage.deleteFile(bucketName, storageKey);
    }
}
