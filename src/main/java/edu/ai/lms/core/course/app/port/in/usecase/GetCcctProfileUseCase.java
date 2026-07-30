package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctProfile;

public interface GetCcctProfileUseCase {
    CcctProfile get(UUID courseId, UUID profileId);

    CcctProfile getPublished(UUID courseId);
}
