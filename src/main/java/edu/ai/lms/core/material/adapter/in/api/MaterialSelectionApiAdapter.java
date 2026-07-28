package edu.ai.lms.core.material.adapter.in.api;

import edu.ai.lms.core.material.api.MaterialEligibility;
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
        return inspect(materialIds, courseId, true);
    }

    @Override
    public List<MaterialEligibility> inspectUploadedMaterials(UUID courseId, List<UUID> materialIds) {
        return inspect(materialIds, courseId, false);
    }

    private List<MaterialEligibility> inspect(List<UUID> materialIds, UUID courseId, boolean requireReadyPdf) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialIds.stream().distinct().map(materialId -> {
            Material material = materialPort.findByIdAndCourseId(materialId, courseId).orElse(null);
            if (material == null) {
                return new MaterialEligibility(materialId, false, "Material does not belong to the course");
            }
            if (requireReadyPdf && material.getType() != Material.MaterialType.PDF) {
                return new MaterialEligibility(materialId, false, "Only PDF materials can be used for quiz generation");
            }
            if (material.getStatus() != Material.MaterialStatus.UPLOADED
                    || (requireReadyPdf && material.getIngestionStatus() != Material.IngestionStatus.READY)) {
                return new MaterialEligibility(materialId, false, "Material is not ready for quiz generation");
            }
            return new MaterialEligibility(materialId, true, null);
        }).toList();
    }
}
