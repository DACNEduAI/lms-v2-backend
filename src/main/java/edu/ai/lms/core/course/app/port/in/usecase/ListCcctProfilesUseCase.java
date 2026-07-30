package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctProfile;

public interface ListCcctProfilesUseCase {
    List<CcctProfile> list(UUID courseId);
}
