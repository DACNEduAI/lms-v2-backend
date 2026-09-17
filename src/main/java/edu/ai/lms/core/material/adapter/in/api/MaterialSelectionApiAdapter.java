package edu.ai.lms.core.material.adapter.in.api;

import edu.ai.lms.core.material.api.MaterialEligibility;
import edu.ai.lms.core.material.api.AiReadyMaterialMetadata;
import edu.ai.lms.core.material.api.MaterialSelectionApi;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.out.persistence.GetMaterialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MaterialSelectionApiAdapter implements MaterialSelectionApi {

    private final GetMaterialPort materialPort;

    @Override
    public List<MaterialEligibility> inspectReadyPdfMaterials(UUID courseId, List<UUID> materialIds) {
        return inspect(materialIds, courseId, true, true);
    }

    @Override
    public List<MaterialEligibility> inspectUploadedMaterials(UUID courseId, List<UUID> materialIds) {
        return inspect(materialIds, courseId, false, false);
    }

    @Override
    public List<MaterialEligibility> inspectAiReadyMaterials(UUID courseId, List<UUID> materialIds) {
        return inspect(materialIds, courseId, false, true);
    }

    @Override
    public List<AiReadyMaterialMetadata> listAiReadyMaterials(UUID courseId) {
        return materialPort.findByCourseId(courseId).stream()
                .filter(this::isAiReady)
                .sorted(java.util.Comparator.comparing(Material::getTitle,
                        java.util.Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Material::getId))
                .map(material -> new AiReadyMaterialMetadata(
                        material.getId(), material.getTitle(), material.getOriginalFileName(),
                        material.getType() == null ? null : material.getType().name()))
                .toList();
    }

    private List<MaterialEligibility> inspect(
            List<UUID> materialIds, UUID courseId, boolean requirePdf, boolean requireAiReady) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialIds.stream().distinct().map(materialId -> {
            Material material = materialPort.findByIdAndCourseId(materialId, courseId).orElse(null);
            if (material == null) {
                return new MaterialEligibility(materialId, false, "Material does not belong to the course", null);
            }
            if (requirePdf && material.getType() != Material.MaterialType.PDF) {
                return new MaterialEligibility(materialId, false, "Only PDF materials can be used for quiz generation", null);
            }
            if (material.getStatus() != Material.MaterialStatus.UPLOADED
                    || (requireAiReady && material.getIngestionStatus() != Material.IngestionStatus.READY)) {
                String reason = requireAiReady
                        ? (requirePdf ? "Material is not ready for quiz generation" : "Material is not ready for AI chat")
                        : "Material is not uploaded";
                return new MaterialEligibility(materialId, false, reason, null);
            }
            return new MaterialEligibility(materialId, true, null, material.getFileKey());
        }).toList();
    }

    private boolean isAiReady(Material material) {
        return material.getStatus() == Material.MaterialStatus.UPLOADED
                && material.getIngestionStatus() == Material.IngestionStatus.READY;
    }
}
