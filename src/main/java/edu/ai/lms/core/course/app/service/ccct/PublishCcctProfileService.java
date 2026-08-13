package edu.ai.lms.core.course.app.service.ccct;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.port.in.command.PublishCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.PublishCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublishCcctProfileService implements PublishCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctValidator validator;
    private final CcctProfiles profiles;
    private final SaveCcctProfilePort savePort;
    private final LoadCcctProfilePort loadPort;
    private final CcctActivity activity;

    @Override
    @Transactional
    public CcctProfile publish(PublishCcctProfileCommand command) {
        var context = access.requireCourseTeacher(command.courseId());
        CcctProfile draft = profiles.require(command.courseId(), command.profileId());
        if (draft.getStatus() != CcctProfileStatus.DRAFT) {
            throw new CcctConflictException("Only a DRAFT profile can be published");
        }
        if (draft.getVersion() != command.version()) {
            throw new CcctConflictException(
                    "CCCT draft changed before publish; reload and try again");
        }
        validator.validatePublish(context.course(), draft);

        loadPort.findPublishedByCourseId(command.courseId())
                .map(CcctProfile::getId)
                .ifPresent(publishedProfileId ->
                        savePort.archivePublished(command.courseId(), publishedProfileId));
        if (!savePort.publishDraft(
                command.courseId(),
                command.profileId(),
                command.version(),
                LocalDateTime.now(java.time.Clock.systemUTC()))) {
            throw new CcctConflictException(
                    "CCCT draft changed before publish; reload and try again");
        }
        CcctProfile published = loadPort.findByCourseIdAndId(command.courseId(), command.profileId())
                .orElseThrow(() -> new IllegalStateException(
                        "CCCT profile disappeared after publish"));
        activity.publish(context.actor().id(), "PUBLISH", published);
        return published;
    }
}
