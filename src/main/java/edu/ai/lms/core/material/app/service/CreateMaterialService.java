package edu.ai.lms.core.material.app.service;

import java.util.Locale;

import edu.ai.lms.core.material.app.port.in.usecase.CreateMaterialUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.response.CreateMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.exception.MaterialAlreadyUploadedException;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;
import edu.ai.lms.core.material.app.port.out.persistence.CreateMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.persistence.ModifyMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateUploadLinkPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateMaterialService implements CreateMaterialUseCase {

    private final CreateMaterialPort createMaterialPort;
    private final GetMaterialPort getMaterialPort;
    private final ModifyMaterialPort modifyMaterialPort;
    private final CreateUploadLinkPort createUploadLinkPort;

    @Override
    @Transactional
    public CreateMaterialResponse createMaterialForCourse(CreateMaterialForCourseCommand command) {
        rejectDuplicateUploadedFileName(command.courseId(), command.material());
        CreateMaterialResponse resumed = resumeIncompleteUpload(command.material());
        if (resumed != null) {
            return resumed;
        }

        Material saved = createMaterialPort.createForCourse(
                command.courseId(),
                command.material());

        String presignedUploadUrl = createUploadLinkPort.createUploadLink(
                saved.getFileKey(), saved.getContentType(), null);

        return toResponse(saved, presignedUploadUrl);
    }

    @Override
    @Transactional
    public CreateMaterialResponse createMaterialForChapter(CreateMaterialForChapterCommand command) {
        rejectDuplicateUploadedFileName(command.courseId(), command.material());
        CreateMaterialResponse resumed = resumeIncompleteUpload(command.material());
        if (resumed != null) {
            return resumed;
        }

        Material saved = createMaterialPort.createForChapter(
                command.courseId(),
                command.chapterId(),
                command.material());

        String presignedUploadUrl = createUploadLinkPort.createUploadLink(
                saved.getFileKey(), saved.getContentType(), null);

        return toResponse(saved, presignedUploadUrl);
    }

    private CreateMaterialResponse toResponse(Material m, String presignedUploadUrl) {
        return CreateMaterialResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .originalFileName(m.getOriginalFileName())
                .type(m.getType() != null ? m.getType().name() : null)
                .fileKey(m.getFileKey())
                .isDownloadable(m.isDownloadable())
                .status(m.getStatus() != null ? m.getStatus().name() : "PENDING")
                .ingestionStatus(m.getIngestionStatus() != null ? m.getIngestionStatus().name() : "NOT_STARTED")
                .presignedUploadUrl(presignedUploadUrl)
                .courseId(m.getCourseId())
                .createdAt(m.getCreatedAt())
                .build();
    }



    private CreateMaterialResponse resumeIncompleteUpload(Material requested) {
        Material existing = getMaterialPort.findByFileKey(requested.getFileKey()).orElse(null);
        if (existing == null) {
            return null;
        }

        if (existing.getStatus() == Material.MaterialStatus.UPLOADED) {
            throw new MaterialAlreadyUploadedException(
                    "A material with the same file name has already been uploaded. " +
                    "Use the PUT endpoint to replace the existing file.");
        }

        existing.setTitle(requested.getTitle());
        existing.setType(requested.getType());
        existing.setDownloadable(requested.isDownloadable());
        existing.setStatus(Material.MaterialStatus.PENDING);
        existing.setIngestionStatus(Material.IngestionStatus.NOT_STARTED);
        existing.setIngestionError(null);
        existing.setIngestionRevision(null);
        existing.setIngestedAt(null);

        Material resumed = modifyMaterialPort.update(existing);
        String presignedUploadUrl = createUploadLinkPort.createUploadLink(
                resumed.getFileKey(), resumed.getContentType(), null);

        return toResponse(resumed, presignedUploadUrl);
    }

    private void rejectDuplicateUploadedFileName(java.util.UUID courseId, Material requested) {
        String requestedName = normalizeFileName(requested.getOriginalFileName());
        if (requestedName.isEmpty()) {
            return;
        }
        boolean exists = getMaterialPort.findByCourseId(courseId).stream()
                .anyMatch(existing -> Material.MaterialStatus.UPLOADED.equals(existing.getStatus())
                        && requestedName.equals(normalizeFileName(existing.getOriginalFileName())));
        if (exists) {
            throw new MaterialAlreadyUploadedException(
                    "A file with this name has already been uploaded. Rename the file and try again.");
        }
    }

    private static String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }
}
