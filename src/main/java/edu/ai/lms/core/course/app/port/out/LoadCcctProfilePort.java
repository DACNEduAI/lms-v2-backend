package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctProfile;

public interface LoadCcctProfilePort {
    Optional<CcctProfile> findByCourseIdAndId(UUID courseId, UUID profileId);

    Optional<CcctProfile> findPublishedByCourseId(UUID courseId);

    List<CcctProfile> findAllByCourseId(UUID courseId);

    boolean existsByCourseIdAndNameExcludingId(UUID courseId, String name, UUID excludedProfileId);
}
