package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.CcctProfile;

public interface SaveCcctProfilePort {
    CcctProfile create(CcctProfile profile);

    boolean replaceDraft(CcctProfile profile, long expectedVersion);
}
