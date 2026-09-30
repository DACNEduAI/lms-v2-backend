package edu.ai.lms.core.material.app.service;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.material.adapter.out.storage.S3StorageAdapter;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import edu.ai.lms.core.material.api.StoredObjectMetadata;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseAssetStorageService implements CourseAssetStorageApi {
    private final S3StorageAdapter storage;

    @Override public String createUploadLink(String storageKey, String contentType) { return storage.createUploadLink(storageKey, contentType); }
    @Override public String createDownloadLink(String storageKey) { return storage.createDownloadLink(storageKey, null); }
    @Override public StoredObjectMetadata inspect(String storageKey) { return storage.inspect(storageKey); }
    @Override public byte[] read(String storageKey) { return storage.readFile(storageKey); }
    @Override public void delete(String storageKey) { storage.deleteFile(storageKey); }
}
