package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValidateMaterialSelectionPort {

    Optional<String> findValidationError(UUID courseId, List<UUID> materialIds);
}
