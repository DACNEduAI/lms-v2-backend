package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

public record CloneCcctProfileCommand(UUID courseId, UUID profileId, String name) {
}
