package edu.ai.lms.core.material.app.port.in.usecase;

import edu.ai.lms.core.material.adapter.in.response.GetMaterialResponse;

import java.util.UUID;

public interface GetMaterialUseCase {
    GetMaterialResponse getMaterial(UUID courseId, UUID materialId);
}
