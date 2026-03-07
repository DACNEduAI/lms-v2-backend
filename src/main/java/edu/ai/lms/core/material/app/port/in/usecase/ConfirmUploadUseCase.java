package edu.ai.lms.core.material.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.material.adapter.in.response.ConfirmUploadResponse;

public interface ConfirmUploadUseCase {

    /**
     * Verifies whether the file for the given material has been uploaded to S3.
     * If uploaded, updates the material status to UPLOADED; otherwise marks it as FAILED.
     *
     * @param courseId   the course the material belongs to
     * @param materialId the material whose upload is being confirmed
     * @return confirmation result with updated status
     */
    ConfirmUploadResponse confirmUpload(UUID courseId, UUID materialId);
}
