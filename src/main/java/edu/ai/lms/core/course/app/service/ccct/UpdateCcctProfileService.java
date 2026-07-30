package edu.ai.lms.core.course.app.service.ccct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.port.in.command.UpdateCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.UpdateCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateCcctProfileService implements UpdateCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctValidator validator;
    private final CcctProfiles profiles;
    private final CcctProfileFactory factory;
    private final SaveCcctProfilePort savePort;
    private final LoadCcctProfilePort loadPort;
    private final CcctActivity activity;

    @Override
    @Transactional
    public CcctProfile update(UpdateCcctProfileCommand command) {
        var context = access.requireCourseTeacher(command.courseId());
        CcctProfile current = profiles.require(command.courseId(), command.profileId());
        requireEditableVersion(current, command.version());
        validator.validateDraft(context.course(), command.content());
        String name = validator.normalizeName(command.content().name());
        profiles.requireUniqueName(command.courseId(), name, command.profileId());

        CcctProfile replacement = factory.replacement(
                command.courseId(),
                command.profileId(),
                current.getCreatedBy(),
                command.content());
        if (!savePort.replaceDraft(replacement, command.version())) {
            throw new CcctConflictException(
                    "CCCT draft changed before this update; reload and try again");
        }
        CcctProfile updated = loadPort.findByCourseIdAndId(command.courseId(), command.profileId())
                .orElseThrow(() -> new IllegalStateException(
                        "CCCT profile disappeared after update"));
        activity.publish(context.actor().id(), "UPDATE", updated);
        return updated;
    }

    private void requireEditableVersion(CcctProfile current, long expectedVersion) {
        if (current.getStatus() != CcctProfileStatus.DRAFT) {
            throw new CcctConflictException("Only DRAFT profiles can be updated");
        }
        if (current.getVersion() != expectedVersion) {
            throw new CcctConflictException(
                    "CCCT draft changed before this update; reload and try again");
        }
    }
}
