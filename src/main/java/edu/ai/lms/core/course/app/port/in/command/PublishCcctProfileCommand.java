package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

public record PublishCcctProfileCommand(UUID courseId, UUID profileId, long version) {
}
