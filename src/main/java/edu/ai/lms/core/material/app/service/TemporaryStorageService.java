package edu.ai.lms.core.material.app.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.material.api.TemporaryStorageApi;
import edu.ai.lms.core.material.app.port.out.storage.CreateDownloadLinkPort;
import edu.ai.lms.core.material.app.port.out.storage.DeleteFilePort;
import edu.ai.lms.core.material.app.port.out.storage.UploadFilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemporaryStorageService implements TemporaryStorageApi {
    private final UploadFilePort uploadFilePort;
    private final CreateDownloadLinkPort downloadLinkPort;
    private final DeleteFilePort deleteFilePort;

    @Override
    public void upload(String fileKey, byte[] content, String contentType) {
        uploadFilePort.uploadFile(fileKey, content, contentType, false);
    }

    @Override
    public String createDownloadLink(String fileKey, Duration expiry) {
        return downloadLinkPort.createDownloadLink(fileKey, expiry);
    }

    @Override
    public void delete(String fileKey) {
        deleteFilePort.deleteFile(fileKey);
    }
}
