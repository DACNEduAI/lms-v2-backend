package edu.ai.lms.core.course.app.port.in.command;

import java.util.List;
import java.util.UUID;

public record CcctTopicValue(UUID id, String title, List<CcctItemValue> items) {
    public CcctTopicValue {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
