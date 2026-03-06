package edu.ai.lms.core.material.app.port.in.usecase;

import edu.ai.lms.core.material.adapter.in.response.ModifyMaterialResponse;
import edu.ai.lms.core.material.app.port.in.command.ModifyMaterialCommand;

public interface ModifyMaterialUseCase {
    ModifyMaterialResponse modifyMaterial(ModifyMaterialCommand command);
}
