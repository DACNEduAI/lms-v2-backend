package edu.ai.lms.core.material.api;

import java.util.List;
import java.util.UUID;

public interface MaterialSelectionApi {

    List<MaterialEligibility> inspectReadyPdfMaterials(UUID courseId, List<UUID> materialIds);

    List<MaterialEligibility> inspectUploadedMaterials(UUID courseId, List<UUID> materialIds);
}
