package edu.ai.lms.core.course.app.service.ccct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.port.in.command.CreateCcctProfileCommand;
import edu.ai.lms.core.course.app.port.in.usecase.CreateCcctProfileUseCase;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCcctProfileService implements CreateCcctProfileUseCase {
    private final CcctAccess access;
    private final CcctValidator validator;
    private final CcctProfiles profiles;
    private final CcctProfileFactory factory;
    private final SaveCcctProfilePort savePort;
    private final CcctActivity activity;

    @Override
    @Transactional
    public CcctProfile create(CreateCcctProfileCommand command) {
        var context = access.requireCourseTeacher(command.courseId());
        validator.validateDraft(context.course(), command.content());
        String name = validator.normalizeName(command.content().name());
        profiles.requireUniqueName(command.courseId(), name, null);
        CcctProfile draft = factory.createDraft(
                command.courseId(),
                context.actor().id(),
                command.content());
        CcctProfile saved = savePort.create(draft);
        activity.publish(context.actor().id(), "CREATE", saved);
        return saved;
    }
}
