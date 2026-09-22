package edu.ai.lms.core.quiz3.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.api.GetPublicFileUrlApi;
import edu.ai.lms.core.material.api.UploadStorageFileApi;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionImagePublicUrlPort;
import edu.ai.lms.core.quiz3.app.port.out.UploadQuestionImagePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuestionImageStorageAdapter implements UploadQuestionImagePort, LoadQuestionImagePublicUrlPort {

    private final UploadStorageFileApi uploadStorageFileApi;
    private final GetPublicFileUrlApi getPublicFileUrlApi;

    @Override
    public void uploadQuestionImage(String fileKey, byte[] content, String contentType, boolean isPublic) {
        uploadStorageFileApi.uploadFile(fileKey, content, contentType, isPublic);
    }

    @Override
    public String getPublicImageUrl(String fileKey) {
        return getPublicFileUrlApi.getPublicFileUrl(fileKey);
    }
}
