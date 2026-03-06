package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.database.MaterialJdbcTable;

@Component
public class MaterialJdbcMapper {

    public MaterialJdbcTable mapToMaterialJdbcEntity(Material material) {
        return MaterialJdbcTable.builder()
                .id(material.getId())
                .title(material.getTitle())
                .type(mapToJdbcMaterialType(material.getType()))
                .fileKey(material.getFileKey())
                .isDownloadable(material.isDownloadable())
                .build();
    }

    public Material mapToMaterial(MaterialJdbcTable entity) {
        return Material.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .type(mapToDomainMaterialType(entity.getType()))
                .fileKey(entity.getFileKey())
                .downloadable(entity.isDownloadable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MaterialJdbcTable.MaterialType mapToJdbcMaterialType(Material.MaterialType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PDF -> MaterialJdbcTable.MaterialType.PDF;
            case VIDEO -> MaterialJdbcTable.MaterialType.VIDEO;
        };
    }

    private Material.MaterialType mapToDomainMaterialType(MaterialJdbcTable.MaterialType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PDF -> Material.MaterialType.PDF;
            case VIDEO -> Material.MaterialType.VIDEO;
        };
    }
}
