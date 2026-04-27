package edu.ai.lms.core.material.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.material.adapter.in.response.GetMaterialResponse;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.port.in.usecase.GetChapterMaterialsUseCase;
import edu.ai.lms.core.material.app.port.in.usecase.GetMaterialUseCase;
import edu.ai.lms.core.material.app.port.in.usecase.GetUploadedMaterialsUseCase;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import edu.ai.lms.core.material.app.port.out.storage.CreateDownloadLinkPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMaterialService implements GetMaterialUseCase, GetUploadedMaterialsUseCase, GetChapterMaterialsUseCase {

    private final GetMaterialPort getMaterialPort;
    private final CreateDownloadLinkPort createDownloadLinkPort;

    @Override
    @Transactional(readOnly = true)
    public GetMaterialResponse getMaterial(UUID courseId, UUID materialId) {
        Material material = getMaterialPort.findByIdAndCourseId(materialId, courseId)
                .orElseThrow(() -> new MaterialNotFoundException(
                        "Material not found with id: " + materialId + " in course: " + courseId));

        String presignedUrl = null;
        if (material.getFileKey() != null && material.isDownloadable()) {
            presignedUrl = createDownloadLinkPort.createDownloadLink(material.getFileKey(), null);
        }

        return GetMaterialResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .type(material.getType() != null ? material.getType().name() : null)
                .fileKey(material.getFileKey())
                .isDownloadable(material.isDownloadable())
                .status(material.getStatus() != null ? material.getStatus().name() : null)
                .presignedUrl(presignedUrl)
                .courseId(material.getCourseId())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetMaterialResponse> getUploadedMaterials(UUID courseId) {
        return getMaterialPort.findByCourseId(courseId)
                .stream()
                .filter(m -> Material.MaterialStatus.UPLOADED.equals(m.getStatus()))
                .map(m -> GetMaterialResponse.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .type(m.getType() != null ? m.getType().name() : null)
                        .fileKey(m.getFileKey())
                        .isDownloadable(m.isDownloadable())
                        .status(m.getStatus().name())
                        .courseId(m.getCourseId())
                        .createdAt(m.getCreatedAt())
                        .updatedAt(m.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetMaterialResponse> getChapterMaterials(UUID courseId, UUID chapterId) {
        return getMaterialPort.findByChapter(chapterId, courseId)
                .stream()
                .map(m -> GetMaterialResponse.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .type(m.getType() != null ? m.getType().name() : null)
                        .fileKey(m.getFileKey())
                        .isDownloadable(m.isDownloadable())
                        .status(m.getStatus() != null ? m.getStatus().name() : null)
                        .courseId(m.getCourseId())
                        .createdAt(m.getCreatedAt())
                        .updatedAt(m.getUpdatedAt())
                        .build())
                .toList();
    }
}
