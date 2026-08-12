package edu.ai.lms.core.quiz.adapter.out.storage;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.api.GetPublicFileUrlApi;
import edu.ai.lms.core.material.api.UploadStorageFileApi;
import edu.ai.lms.core.material.app.port.out.storage.DeleteFilePort;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RichContentAssetStorageAdapter implements RichContentAssetStoragePort {
    private final UploadStorageFileApi uploadStorageFileApi;
    private final GetPublicFileUrlApi getPublicFileUrlApi;
    private final DeleteFilePort deleteFilePort;

    @Override
    public void upload(String storageKey, byte[] content, String mediaType) {
        uploadStorageFileApi.uploadFile(storageKey, content, mediaType, true);
    }

    @Override
    public String publicUrl(String storageKey) {
        return getPublicFileUrlApi.getPublicFileUrl(storageKey);
    }

    @Override
    public void delete(String storageKey) {
        deleteFilePort.deleteFile(storageKey);
    }
}
