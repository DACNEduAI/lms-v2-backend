package edu.ai.lms.core.material.events;

import java.util.UUID;

public record MaterialUploadConfirmedEvent(UUID courseId, UUID materialId, String materialTitle) {
}
