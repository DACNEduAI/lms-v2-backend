package edu.ai.lms.core.chat.app.port.out;

import java.util.List;
import java.util.UUID;

public interface ValidateChatMaterialsPort {
    boolean allAvailableInCourse(UUID courseId, List<UUID> materialIds);
}
