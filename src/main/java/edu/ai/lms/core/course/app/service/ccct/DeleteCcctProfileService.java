package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.port.in.usecase.DeleteCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCcctProfileService implements DeleteCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctProfiles profiles;
    private final DeleteCcctProfilePort deletePort;
    private final CcctActivity activity;

    @Override
    @Transactional
    public void delete(UUID courseId, UUID profileId) {
        var context = access.requireCourseTeacher(courseId);
        CcctProfile profile = profiles.require(courseId, profileId);
        if (profile.getStatus() != CcctProfileStatus.DRAFT) {
            throw new CcctConflictException("Only DRAFT profiles can be deleted");
        }
        if (!deletePort.deleteDraft(courseId, profileId)) {
            throw new CcctConflictException(
                    "CCCT profile changed before deletion; reload and try again");
        }
        activity.publish(context.actor().id(), "DELETE", profile);
    }
}
