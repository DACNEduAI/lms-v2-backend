package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.exception.CcctProfileNotFoundException;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CcctProfiles {
    private final LoadCcctProfilePort loadPort;

    CcctProfile require(UUID courseId, UUID profileId) {
        return loadPort.findByCourseIdAndId(courseId, profileId)
                .orElseThrow(() -> new CcctProfileNotFoundException(
                        "CCCT profile not found in Course: " + profileId));
    }

    void requireUniqueName(UUID courseId, String name, UUID excludedProfileId) {
        if (loadPort.existsByCourseIdAndNameExcludingId(courseId, name, excludedProfileId)) {
            throw new CcctConflictException(
                    "A CCCT profile with this name already exists in the Course");
        }
    }

    List<CcctProfile> list(UUID courseId) { return loadPort.findAllByCourseId(courseId); }
}
