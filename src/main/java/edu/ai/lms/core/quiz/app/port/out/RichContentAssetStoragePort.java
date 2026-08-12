package edu.ai.lms.core.quiz.app.port.out;

public interface RichContentAssetStoragePort {
    void upload(String storageKey, byte[] content, String mediaType);

    String publicUrl(String storageKey);
}
