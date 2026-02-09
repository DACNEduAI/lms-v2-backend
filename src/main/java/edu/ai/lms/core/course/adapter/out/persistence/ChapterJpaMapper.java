package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.database.ChapterJpaEntity;
import edu.ai.lms.database.CourseJpaEntity;
import edu.ai.lms.database.MaterialJpaEntity;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChapterJpaMapper {

    private final MaterialJpaMapper materialJpaMapper;

    public ChapterJpaEntity mapToChapterJpaEntity(Chapter chapter, CourseJpaEntity courseEntity) {
        ChapterJpaEntity chapterEntity = ChapterJpaEntity.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .course(courseEntity)
                .materials(new ArrayList<>())
                .build();

        // Map materials
        if (chapter.getMaterials() != null) {
            List<MaterialJpaEntity> materialEntities = chapter.getMaterials().stream()
                    .map(material -> materialJpaMapper.mapToMaterialJpaEntity(material, chapterEntity))
                    .collect(Collectors.toList());
            chapterEntity.setMaterials(materialEntities);
        }

        return chapterEntity;
    }

    public Chapter mapToChapter(ChapterJpaEntity entity, Course course) {
        Chapter chapter = Chapter.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .orderIndex(entity.getOrderIndex())
                .course(course)
                .materials(new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Map materials
        if (entity.getMaterials() != null) {
            List<Material> materials = entity.getMaterials().stream()
                    .map(materialEntity -> materialJpaMapper.mapToMaterial(materialEntity, chapter))
                    .collect(Collectors.toList());
            chapter.setMaterials(materials);
        }

        return chapter;
    }
}
