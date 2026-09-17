package edu.ai.lms.core.chat.adapter.out.integration;

import edu.ai.lms.core.chat.app.port.out.ValidateChatMaterialsPort;
import edu.ai.lms.core.material.api.MaterialSelectionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ChatMaterialAdapter implements ValidateChatMaterialsPort {

    private final MaterialSelectionApi materialSelectionApi;

    @Override
    public boolean allAvailableInCourse(UUID courseId, List<UUID> materialIds) {
        return materialSelectionApi.inspectAiReadyMaterials(courseId, materialIds).stream()
                .allMatch(result -> result.eligible());
    }
}
