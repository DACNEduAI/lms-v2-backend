package edu.ai.lms.core.material.app.port.in.usecase;

import edu.ai.lms.core.material.adapter.in.response.DeleteMaterialResponse;

import java.util.UUID;

public interface DeleteMaterialUseCase {
    DeleteMaterialResponse deleteMaterial(UUID courseId, UUID materialId);
}
