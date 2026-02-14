package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.database.MaterialJdbcEntity;

@Component
public class MaterialJdbcMapper {

    public MaterialJdbcEntity mapToMaterialJdbcEntity(Material material) {
        return MaterialJdbcEntity.builder()
                .id(material.getId())
                .title(material.getTitle())
                .type(mapToJdbcMaterialType(material.getType()))
                .sourceUrl(material.getSourceUrl())
                .isDownloadable(material.isDownloadable())
                .chapterId(material.getChapter() != null ? material.getChapter().getId() : null)
                .build();
    }

    public Material mapToMaterial(MaterialJdbcEntity entity) {
        return Material.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .type(mapToDomainMaterialType(entity.getType()))
                .sourceUrl(entity.getSourceUrl())
                .downloadable(entity.isDownloadable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MaterialJdbcEntity.MaterialType mapToJdbcMaterialType(Material.MaterialType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PDF -> MaterialJdbcEntity.MaterialType.PDF;
            case VIDEO -> MaterialJdbcEntity.MaterialType.VIDEO;
            case LINK -> MaterialJdbcEntity.MaterialType.LINK;
        };
    }

    private Material.MaterialType mapToDomainMaterialType(MaterialJdbcEntity.MaterialType type) {
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
