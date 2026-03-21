package edu.ai.lms.core.material.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.adapter.in.request.CreateMaterialRequest;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForChapterCommand;
import edu.ai.lms.core.material.app.port.in.command.CreateMaterialForCourseCommand;

@Component
public class CreateMaterialCommandMapper {

    public CreateMaterialForCourseCommand toCommand(UUID courseId, CreateMaterialRequest request) {
        String fileKey = generateFileKey(courseId, request.getTitle(), request.getType());

        Material material = Material.builder()
                .title(request.getTitle())
                .type(request.getType())
                .fileKey(fileKey)
                .downloadable(request.getIsDownloadable() != null && request.getIsDownloadable())
                .build();

        return CreateMaterialForCourseCommand.builder()
                .courseId(courseId)
                .material(material)
                .build();
    }

    public CreateMaterialForChapterCommand toCommand(UUID courseId, UUID chapterId, CreateMaterialRequest request) {
        String fileKey = generateFileKey(courseId, request.getTitle(), request.getType());

        Material material = Material.builder()
                .title(request.getTitle())
                .type(request.getType())
                .fileKey(fileKey)
                .downloadable(request.getIsDownloadable() != null && request.getIsDownloadable())
                .build();

        return CreateMaterialForChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .material(material)
                .build();
    }

    private String generateFileKey(UUID courseId, String title, Material.MaterialType type) {
        String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = resolveFileExtension(type);
        return String.format("courses/%s/%s.%s", courseId, sanitizedTitle, extension);
    }

    private String resolveFileExtension(Material.MaterialType type) {
        if (type == null) return "bin";
        return switch (type) {
            case PDF -> "pdf";
            case VIDEO -> "mp4";
        };
    }
}
