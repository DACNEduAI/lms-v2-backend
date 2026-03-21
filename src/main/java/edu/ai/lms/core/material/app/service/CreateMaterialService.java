package edu.ai.lms.core.material.app.service;

import edu.ai.lms.core.material.app.port.in.usecase.CreateMaterialUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.exception.MaterialAlreadyUploadedException;
import edu.ai.lms.core.material.app.exception.MaterialUploadPendingException;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;
import edu.ai.lms.core.material.app.port.out.persistence.CreateMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateUploadLinkPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateMaterialService implements CreateMaterialUseCase {

    private final CreateMaterialPort createMaterialPort;
    private final GetMaterialPort getMaterialPort;
    private final CreateUploadLinkPort createUploadLinkPort;

    @Override
    @Transactional
    public CreateMaterialResponse createMaterialForCourse(CreateMaterialForCourseCommand command) {
        checkForDuplicateFileKey(command.material().getFileKey());

        Material saved = createMaterialPort.createForCourse(
                command.courseId(),
                command.material());

        String presignedUploadUrl = createUploadLinkPort.createUploadLink(
                saved.getFileKey(), resolveContentType(saved));

        return toResponse(saved, presignedUploadUrl);
    }

    @Override
    @Transactional
    public CreateMaterialResponse createMaterialForChapter(CreateMaterialForChapterCommand command) {
        checkForDuplicateFileKey(command.material().getFileKey());

        Material saved = createMaterialPort.createForChapter(
                command.courseId(),
                command.chapterId(),
                command.material());

        String presignedUploadUrl = createUploadLinkPort.createUploadLink(
                saved.getFileKey(), resolveContentType(saved));

        return toResponse(saved, presignedUploadUrl);
    }

    private CreateMaterialResponse toResponse(Material m, String presignedUploadUrl) {
        return CreateMaterialResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .type(m.getType() != null ? m.getType().name() : null)
                .fileKey(m.getFileKey())
                .isDownloadable(m.isDownloadable())
                .status(m.getStatus() != null ? m.getStatus().name() : "PENDING")
                .presignedUploadUrl(presignedUploadUrl)
                .courseId(m.getCourseId())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String resolveContentType(Material m) {
        if (m.getType() == null) return "application/octet-stream";
        return switch (m.getType()) {
            case PDF -> "application/pdf";
            case VIDEO -> "video/mp4";
        };
    }

    private void checkForDuplicateFileKey(String fileKey) {
        getMaterialPort.findByFileKey(fileKey).ifPresent(existing -> {
            if (existing.getStatus() == Material.MaterialStatus.UPLOADED) {
                throw new MaterialAlreadyUploadedException(
                        "A material with the same file name has already been uploaded. " +
                        "Use the PUT endpoint to replace the existing file.");
            }
            if (existing.getStatus() == Material.MaterialStatus.PENDING) {
                throw new MaterialUploadPendingException(
                        "A material with the same file name already exists but its upload status is unknown. " +
                        "Use the PATCH endpoint to check the upload status of the file.");
            }
        });
    }
}
