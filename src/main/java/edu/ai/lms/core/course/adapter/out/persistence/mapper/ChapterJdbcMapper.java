package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.database.ChapterJdbcEntity;
import edu.ai.lms.database.MaterialJdbcEntity;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChapterJdbcMapper {

    private final MaterialJdbcMapper materialJdbcMapper;

    public ChapterJdbcEntity mapToChapterJdbcEntity(Chapter chapter, UUID courseId) {
        return ChapterJdbcEntity.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .courseId(courseId)
                .build();
    }

    /// Map to chapter WITHOUT Materials
    public Chapter mapToChapterWithoutMaterials(ChapterJdbcEntity entity) {
        return Chapter.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderIndex(entity.getOrderIndex())
                .courseId(entity.getCourseId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    /// Map to chapter WITH Materials
    public Chapter mapToChapter(ChapterJdbcEntity entity, List<MaterialJdbcEntity> materialEntities) {
        Chapter chapter = Chapter.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderIndex(entity.getOrderIndex())
                .courseId(entity.getCourseId())
                .materials(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        if (materialEntities != null) {
            List<Material> materials = materialEntities.stream()
                    .map(materialJdbcMapper::mapToMaterial)
                    .collect(Collectors.toList());
            chapter.setMaterials(materials);
        }

        return chapter;
    }
}
