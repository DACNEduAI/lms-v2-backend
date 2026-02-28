package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.ModifyChapterRequest;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.core.course.app.port.in.command.ModifyChapterCommand;

@Component
public class ModifyChapterCommandMapper {

    public ModifyChapterCommand toCommand(UUID courseId, Long chapterNumber, ModifyChapterRequest request) {
        List<Material> materials = request.getMaterials().stream()
                .map(materialRequest -> Material.builder()
                        .title(materialRequest.getTitle())
                        .type(materialRequest.getType())
                        .sourceUrl(materialRequest.getSourceUrl())
                        .downloadable(materialRequest.isDownloadable())
                        .build())
                .collect(Collectors.toList());

        Chapter chapter = Chapter.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .materials(materials)
                .build();

        return ModifyChapterCommand.builder()
                .courseId(courseId)
                .chapterNumber(chapterNumber)
                .chapter(chapter)
                .build();
    }
}
