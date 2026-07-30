package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.exception.CcctProfileNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCcctProfileService implements GetCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctProfiles profiles;
    private final LoadCcctProfilePort loadPort;

    @Override
    @Transactional(readOnly = true)
    public CcctProfile get(UUID courseId, UUID profileId) {
        access.requireCourseTeacher(courseId);
        return profiles.require(courseId, profileId);
    }

    @Override
    @Transactional(readOnly = true)
    public CcctProfile getPublished(UUID courseId) {
        access.requireCourseTeacher(courseId);
        return loadPort.findPublishedByCourseId(courseId)
                .orElseThrow(() -> new CcctProfileNotFoundException(
                        "Published CCCT profile not found in Course: " + courseId));
    }
}
