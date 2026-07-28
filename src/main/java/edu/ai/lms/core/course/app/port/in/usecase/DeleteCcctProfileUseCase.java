package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

public interface DeleteCcctProfileUseCase {
    void delete(UUID courseId, UUID profileId);
}
