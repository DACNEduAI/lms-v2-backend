package edu.ai.lms.core.course.app.port.in.command;

import java.util.UUID;

import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;

public record CcctItemValue(
        UUID id,
        String content,
        CcctCoverageStatus status,
        String note) {
}
