package edu.ai.lms.core.course.api;

import java.util.Optional;
import java.util.UUID;

/** Course-owned metadata boundary for the AI chat module. */
public interface CourseChatMetadataApi {
    Optional<CourseChatMetadata> findForChat(UUID courseId);
}
