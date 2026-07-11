package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.GetGroupLearningOutcomeResponse;
import edu.ai.lms.core.course.adapter.in.response.GetLearningOutcomeResponse;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetGroupLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.GetLearningOutcomeUseCase;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetLearningOutcomeService implements GetLearningOutcomeUseCase, GetGroupLearningOutcomeUseCase {

    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;

    @Override
    @Transactional(readOnly = true)
    public GetLearningOutcomeResponse getLearningOutcome(UUID courseId, UUID learningOutcomeId) {
        LearningOutcome learningOutcome = loadLearningOutcomePort.findById(learningOutcomeId)
                .filter(lo -> courseId.equals(lo.getCourseId()))
                .orElseThrow(() -> new LearningOutcomeNotFoundException(
                        "Learning outcome not found with id: " + learningOutcomeId));

        return toLearningOutcomeResponse(learningOutcome);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetLearningOutcomeResponse> getLearningOutcomesByIds(UUID courseId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> requestedIds = Set.copyOf(learningOutcomeIds);
        List<GetLearningOutcomeResponse> responses = loadLearningOutcomePort.findByCourseId(courseId).stream()
                .filter(lo -> requestedIds.contains(lo.getId()))
                .map(this::toLearningOutcomeResponse)
                .toList();

        if (responses.size() != requestedIds.size()) {
            Set<UUID> foundIds = responses.stream().map(GetLearningOutcomeResponse::getId).collect(Collectors.toSet());
            UUID missingId = requestedIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new LearningOutcomeNotFoundException("Learning outcome not found with id: " + missingId);
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetGroupLearningOutcomeResponse> getGroupedLearningOutcomes(UUID courseId) {
        List<GroupLearningOutcome> groups = loadGroupLearningOutcomePort.findByCourseId(courseId);

        return groups.stream()
                .map(group -> GetGroupLearningOutcomeResponse.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .code(group.getCode())
                        .sortOrder(group.getSortOrder())
                        .displayCode(group.getCode())
                        .description(group.getDescription())
                        .courseId(group.getCourseId())
                        .learningOutcomes(group.getLearningOutcomes().stream()
                                .filter(Objects::nonNull)
                                .map(lo -> toLearningOutcomeResponse(lo, group))
                                .toList())
                        .build())
                .toList();
    }

    private GetLearningOutcomeResponse toLearningOutcomeResponse(LearningOutcome learningOutcome) {
        return GetLearningOutcomeResponse.builder()
                .id(learningOutcome.getId())
                .code(learningOutcome.getCode())
                .prefixNumber(learningOutcome.getPrefixNumber())
                .sortOrder(learningOutcome.getSortOrder())
                .displayCode(learningOutcome.getCode())
                .description(learningOutcome.getDescription())
                .courseId(learningOutcome.getCourseId())
                .groupLearningOutcomeId(learningOutcome.getGroupLearningOutcomeId())
                .createdAt(learningOutcome.getCreatedAt())
                .updatedAt(learningOutcome.getUpdatedAt())
                .build();
    }

    private GetLearningOutcomeResponse toLearningOutcomeResponse(LearningOutcome learningOutcome, GroupLearningOutcome group) {
        return GetLearningOutcomeResponse.builder()
                .id(learningOutcome.getId())
                .code(learningOutcome.getCode())
                .prefixNumber(learningOutcome.getPrefixNumber())
                .sortOrder(learningOutcome.getSortOrder())
                .displayCode(group.getCode() + "." + learningOutcome.getPrefixNumber())
                .description(learningOutcome.getDescription())
                .courseId(learningOutcome.getCourseId())
                .groupLearningOutcomeId(learningOutcome.getGroupLearningOutcomeId())
                .createdAt(learningOutcome.getCreatedAt())
                .updatedAt(learningOutcome.getUpdatedAt())
                .build();
    }
}
