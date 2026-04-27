package edu.ai.lms.core.material.app.service;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.material.adapter.in.internal.UploadStorageFileApi;
import edu.ai.lms.core.material.app.port.out.storage.UploadFilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadStorageFileService implements UploadStorageFileApi {

    private final UploadFilePort uploadFilePort;

    @Override
    public void uploadFile(String fileKey, byte[] content, String contentType, boolean isPublic) {
        uploadFilePort.uploadFile(fileKey, content, contentType, isPublic);
    }
}
