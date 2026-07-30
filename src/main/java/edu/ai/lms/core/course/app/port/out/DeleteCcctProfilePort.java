package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

public interface DeleteCcctProfilePort {
    boolean deleteDraft(UUID courseId, UUID profileId);
}
