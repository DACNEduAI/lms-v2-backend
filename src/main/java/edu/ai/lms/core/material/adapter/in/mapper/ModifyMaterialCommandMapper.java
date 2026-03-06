package edu.ai.lms.core.material.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.material.adapter.in.request.ModifyMaterialRequest;
import edu.ai.lms.core.material.app.domain.Material;
import edu.ai.lms.core.material.app.port.in.command.ModifyMaterialCommand;

@Component
public class ModifyMaterialCommandMapper {

    public ModifyMaterialCommand toCommand(UUID courseId, UUID materialId, ModifyMaterialRequest request) {
        Material material = Material.builder()
                .title(request.getTitle())
                .type(request.getType() != null ? Material.MaterialType.valueOf(request.getType()) : null)
                .downloadable(request.getIsDownloadable() != null && request.getIsDownloadable())
                .build();

        return ModifyMaterialCommand.builder()
                .courseId(courseId)
                .materialId(materialId)
                .material(material)
                .build();
    }
}
