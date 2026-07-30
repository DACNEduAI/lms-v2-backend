package edu.ai.lms.core.course.app.service.ccct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.port.in.command.CloneCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.CloneCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloneCcctProfileService implements CloneCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctValidator validator;
    private final CcctProfiles profiles;
    private final CcctProfileFactory factory;
    private final SaveCcctProfilePort savePort;
    private final CcctActivity activity;

    @Override
    @Transactional
    public CcctProfile cloneProfile(CloneCcctProfileCommand command) {
        var context = access.requireCourseTeacher(command.courseId());
        CcctProfile source = profiles.require(command.courseId(), command.profileId());
        String name = validator.normalizeName(command.name());
        profiles.requireUniqueName(command.courseId(), name, null);
        CcctProfile clone = factory.cloneDraft(
                command.courseId(),
                context.actor().id(),
                name,
                source);
        CcctProfile saved = savePort.create(clone);
        activity.publish(context.actor().id(), "CLONE", saved);
        return saved;
    }
}
