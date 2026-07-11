package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.request.ReorderLearningOutcomeTreeRequest;
import edu.ai.lms.core.course.app.port.in.command.ReorderLearningOutcomeTreeCommand;

@Component
public class ReorderLearningOutcomeTreeCommandMapper {

    public ReorderLearningOutcomeTreeCommand toCommand(UUID courseId, ReorderLearningOutcomeTreeRequest request) {
        return ReorderLearningOutcomeTreeCommand.builder()
                .courseId(courseId)
                .groups(request.getGroups().stream()
                        .map(group -> ReorderLearningOutcomeTreeCommand.GroupItem.builder()
                                .groupLearningOutcomeId(group.getGroupLearningOutcomeId())
                                .learningOutcomeIds(group.getLearningOutcomeIds())
                                .build())
                        .toList())
                .build();
    }
}
