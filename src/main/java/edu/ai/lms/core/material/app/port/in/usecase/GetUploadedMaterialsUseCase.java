package edu.ai.lms.core.material.app.port.in.usecase;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.material.adapter.in.response.GetMaterialResponse;

public interface GetUploadedMaterialsUseCase {
    List<GetMaterialResponse> getUploadedMaterials(UUID courseId);
}
