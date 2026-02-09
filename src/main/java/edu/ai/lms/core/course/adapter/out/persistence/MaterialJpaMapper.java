package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.database.ChapterJpaEntity;
import edu.ai.lms.database.MaterialJpaEntity;

@Component
public class MaterialJpaMapper {

    public MaterialJpaEntity mapToMaterialJpaEntity(Material material, ChapterJpaEntity chapterEntity) {
        return MaterialJpaEntity.builder()
                .id(material.getId())
                .title(material.getTitle())
                .type(mapToJpaMaterialType(material.getType()))
                .sourceUrl(material.getSourceUrl())
                .isDownloadable(material.isDownloadable())
                .chapter(chapterEntity)
                .build();
    }

    public Material mapToMaterial(MaterialJpaEntity entity, Chapter chapter) {
        return Material.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .type(mapToDomainMaterialType(entity.getType()))
                .sourceUrl(entity.getSourceUrl())
                .downloadable(entity.isDownloadable())
                .chapter(chapter)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MaterialJpaEntity.MaterialType mapToJpaMaterialType(Material.MaterialType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PDF -> MaterialJpaEntity.MaterialType.PDF;
            case VIDEO -> MaterialJpaEntity.MaterialType.VIDEO;
            case LINK -> MaterialJpaEntity.MaterialType.LINK;
        };
    }

    private Material.MaterialType mapToDomainMaterialType(MaterialJpaEntity.MaterialType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PDF -> Material.MaterialType.PDF;
            case VIDEO -> Material.MaterialType.VIDEO;
            case LINK -> Material.MaterialType.LINK;
        };
    }
}
