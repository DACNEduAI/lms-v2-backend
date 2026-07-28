package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctProfile;

public interface LoadCcctProfilePort {
    Optional<CcctProfile> findByCourseIdAndId(UUID courseId, UUID profileId);
}
