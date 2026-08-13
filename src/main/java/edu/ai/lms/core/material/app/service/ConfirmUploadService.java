package edu.ai.lms.core.material.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.core.material.adapter.in.response.ConfirmUploadResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.in.usecase.ConfirmUploadUseCase;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.VerifyFileUploadedPort;
import edu.ai.lms.core.material.app.port.out.persistence.LoadSignedInUserPort;
import edu.ai.lms.core.material.events.MaterialUploadConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfirmUploadService implements ConfirmUploadUseCase {

    private final GetMaterialPort getMaterialPort;
    private final ModifyMaterialPort modifyMaterialPort;
    private final VerifyFileUploadedPort verifyFileUploadedPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final MaterialIndexingService materialIndexingService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ConfirmUploadResponse confirmUpload(UUID courseId, UUID materialId) {
        Material material = getMaterialPort.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new MaterialNotFoundException(
                        "Material not found with id: " + materialId + " in course: " + courseId));

        boolean uploaded = verifyFileUploadedPort.isFileUploaded(material.getFileKey());

        if (uploaded) {
            material.setStatus(Material.MaterialStatus.UPLOADED);
            material.setIngestionError(null);
            material.setIngestedAt(null);
            if (material.getType() == Material.MaterialType.PDF) {
                material.setIngestionStatus(Material.IngestionStatus.PENDING);
                material.setIngestionRevision(UUID.randomUUID());
            } else {
                material.setIngestionStatus(Material.IngestionStatus.NOT_APPLICABLE);
                material.setIngestionRevision(null);
            }
        } else {
            material.setStatus(Material.MaterialStatus.FAILED);
            material.setIngestionStatus(Material.IngestionStatus.NOT_STARTED);
        }

        Material updated = modifyMaterialPort.update(material);

        ConfirmUploadResponse response = ConfirmUploadResponse.builder()
                .materialId(updated.getId())
                .courseId(updated.getCourseId())
                .fileKey(updated.getFileKey())
                .status(updated.getStatus().name())
                .ingestionStatus(updated.getIngestionStatus().name())
                .uploaded(uploaded)
                .message(uploaded
                        ? "File uploaded successfully. Status updated to UPLOADED."
                        : "File not found in storage. Status updated to FAILED.")
                .build();

        // Broadcast notification if upload was successful
        if (uploaded) {
            // Log activity (Lịch sử hoạt động)
            UUID currentUserId = loadSignedInUserPort.getAuthenticatedUser()
                    .map(u -> u.getId())
                    .orElse(courseId); // Fallback to courseId if not auth (should not happen)

            eventPublisher.publishEvent(SystemActivityEvent.builder()
                    .userId(currentUserId)
                    .actionType("CREATE")
                    .entityType("MATERIAL")
                    .entityId(updated.getId())
                    .entityName(updated.getTitle())
                    .description("Tài liệu mới đã được tải lên thành công")
                    .timestamp(LocalDateTime.now(java.time.Clock.systemUTC()))
                    .build());

            eventPublisher.publishEvent(new MaterialUploadConfirmedEvent(
                    courseId, updated.getId(), updated.getTitle()));
            
            if (updated.getType() == Material.MaterialType.PDF) {
                materialIndexingService.queueIngest(updated);
            }
        }

        return response;
    }
}
