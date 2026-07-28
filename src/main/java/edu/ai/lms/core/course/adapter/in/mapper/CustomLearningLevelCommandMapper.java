package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.*;
import edu.ai.lms.core.course.app.port.in.command.BatchEditCustomLearningLevelsCommand;
import edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue;

@Component
public class CustomLearningLevelCommandMapper {
    public CustomLearningLevelValue toValue(CreateCustomLearningLevelRequest request) {
        return new CustomLearningLevelValue(null, null, request.getLearningOutcomeIds(),
                request.getCode(), request.getName(), request.getDescription(), request.getSortOrder());
    }

    public BatchEditCustomLearningLevelsCommand toCommand(
            UUID courseId,
            UUID matrixId,
            BatchEditCustomLearningLevelsRequest request) {
        List<CustomLearningLevelValue> creates = safe(request.getCreate()).stream()
                .map(value -> new CustomLearningLevelValue(
                        null, value.getClientTempId(), value.getLearningOutcomeIds(),
                        value.getCode(), value.getName(), value.getDescription(), value.getSortOrder()))
                .toList();
        List<CustomLearningLevelValue> updates = safe(request.getUpdate()).stream()
                .map(value -> new CustomLearningLevelValue(
                        value.getId(), null, value.getLearningOutcomeIds(), value.getCode(), value.getName(),
                        value.getDescription(), value.getSortOrder()))
                .toList();
        return new BatchEditCustomLearningLevelsCommand(
                courseId, matrixId, creates, updates, safe(request.getDelete()));
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
