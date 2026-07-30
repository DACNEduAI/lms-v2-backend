package edu.ai.lms.core.course.app.port.out;

import java.time.LocalDateTime;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import java.util.UUID;

public interface SaveCcctProfilePort {
    CcctProfile create(CcctProfile profile);

    boolean replaceDraft(CcctProfile profile, long expectedVersion);

    boolean archivePublished(UUID courseId, UUID publishedProfileId);

    boolean publishDraft(
            UUID courseId,
            UUID profileId,
            long expectedVersion,
            LocalDateTime publishedAt);
}
