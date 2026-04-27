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
        // Use our new domain constructor!
        Material material = new Material(
                courseId,
                request.getTitle(),
                request.getType(),
                request.getIsDownloadable() != null && request.getIsDownloadable()
        );

        return CreateMaterialForCourseCommand.builder()
                .courseId(courseId)
                .material(material)
                .build();
    }

    public CreateMaterialForChapterCommand toCommand(UUID courseId, UUID chapterId, CreateMaterialRequest request) {
        // Use our new domain constructor!
        Material material = new Material(
                courseId,
                request.getTitle(),
                request.getType(),
                request.getIsDownloadable() != null && request.getIsDownloadable()
        );

        return CreateMaterialForChapterCommand.builder()
                .courseId(courseId)
                .chapterId(chapterId)
                .material(material)
                .build();
    }
}