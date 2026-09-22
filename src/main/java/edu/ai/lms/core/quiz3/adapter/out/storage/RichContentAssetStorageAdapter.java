package edu.ai.lms.core.quiz3.adapter.out.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.api.PublicAssetStorageApi;
import edu.ai.lms.core.quiz3.app.port.out.RichContentAssetStoragePort;

@Component
public class RichContentAssetStorageAdapter implements RichContentAssetStoragePort {
    private final PublicAssetStorageApi storage;
    private final String publicBucket;

    public RichContentAssetStorageAdapter(PublicAssetStorageApi storage,
            @Value("${quiz.rich-content.public-bucket}") String publicBucket) {
        if (publicBucket == null || publicBucket.isBlank()) {
            throw new IllegalArgumentException("Rich-content public bucket must be configured");
        }
        this.storage = storage;
        this.publicBucket = publicBucket;
    }

    @Override
    public void upload(String storageKey, byte[] content, String mediaType) {
        storage.upload(publicBucket, storageKey, content, mediaType);
    }

    @Override
    public String publicUrl(String storageKey) {
        return storage.publicUrl(publicBucket, storageKey);
    }

    @Override
    public byte[] read(String storageKey) {
        return storage.read(publicBucket, storageKey);
    }

    @Override
    public void delete(String storageKey) {
        storage.delete(publicBucket, storageKey);
    }
}
