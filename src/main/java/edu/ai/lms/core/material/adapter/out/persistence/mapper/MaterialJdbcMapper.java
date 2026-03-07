package edu.ai.lms.core.material.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.database.MaterialJdbcTable;

@Component
public class MaterialJdbcMapper {

    // ─── Domain → JDBC entity ──────────────────────────────────────────────────

    public MaterialJdbcTable toEntity(Material material, java.util.UUID courseId) {
        return MaterialJdbcTable.builder()
                .id(material.getId())
                .title(material.getTitle())
                .type(toJdbcType(material.getType()))
                .fileKey(material.getFileKey())
                .isDownloadable(material.isDownloadable())
                .status(toJdbcStatus(material.getStatus()))
                .courseId(courseId)
                .createdAt(material.getCreatedAt())
                .build();
    }

    // ─── JDBC entity → Domain ──────────────────────────────────────────────────

    public Material toDomain(MaterialJdbcTable entity) {
        return Material.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .type(toDomainType(entity.getType()))
                .fileKey(entity.getFileKey())
                .downloadable(entity.isDownloadable())
                .status(toDomainStatus(entity.getStatus()))
                .courseId(entity.getCourseId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ─── Enum mapping helpers ──────────────────────────────────────────────────

    private MaterialJdbcTable.MaterialType toJdbcType(Material.MaterialType type) {
        if (type == null) return null;
        return switch (type) {
            case PDF -> MaterialJdbcTable.MaterialType.PDF;
            case VIDEO -> MaterialJdbcTable.MaterialType.VIDEO;
        };
    }

    private Material.MaterialType toDomainType(MaterialJdbcTable.MaterialType type) {
        if (type == null) return null;
        return switch (type) {
            case PDF -> Material.MaterialType.PDF;
            case VIDEO -> Material.MaterialType.VIDEO;
        };
    }

    // ─── Status mapping helpers ────────────────────────────────────────────────

    private MaterialJdbcTable.MaterialStatus toJdbcStatus(Material.MaterialStatus status) {
        if (status == null) return MaterialJdbcTable.MaterialStatus.PENDING;
        return switch (status) {
            case PENDING -> MaterialJdbcTable.MaterialStatus.PENDING;
            case UPLOADED -> MaterialJdbcTable.MaterialStatus.UPLOADED;
            case FAILED -> MaterialJdbcTable.MaterialStatus.FAILED;
        };
    }

    private Material.MaterialStatus toDomainStatus(MaterialJdbcTable.MaterialStatus status) {
        if (status == null) return Material.MaterialStatus.PENDING;
        return switch (status) {
            case PENDING -> Material.MaterialStatus.PENDING;
            case UPLOADED -> Material.MaterialStatus.UPLOADED;
            case FAILED -> Material.MaterialStatus.FAILED;
        };
    }
}
