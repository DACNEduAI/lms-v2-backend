package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import java.util.UUID;

public interface SaveCcctProfilePort {
    CcctProfile create(CcctProfile profile);

    boolean replaceDraft(CcctProfile profile, long expectedVersion);

}
