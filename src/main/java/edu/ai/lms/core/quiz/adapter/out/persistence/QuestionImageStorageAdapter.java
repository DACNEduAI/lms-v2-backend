package edu.ai.lms.core.quiz.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.adapter.in.internal.GetPublicFileUrlApi;
import edu.ai.lms.core.material.adapter.in.internal.UploadStorageFileApi;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionImagePublicUrlPort;
import edu.ai.lms.core.quiz.app.port.out.UploadQuestionImagePort;
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
