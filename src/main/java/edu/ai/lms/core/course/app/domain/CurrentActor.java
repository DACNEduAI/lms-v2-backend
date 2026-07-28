package edu.ai.lms.core.course.app.domain;

import edu.ai.lms.common.api.Roles;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CurrentActor(UUID id, Roles role) {
    public UUID getId() {
        return id;
    }

    public Roles getRole() {
        return role;
    }
}
