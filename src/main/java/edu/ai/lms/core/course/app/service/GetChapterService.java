package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.GetChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllChaptersUseCase;
import edu.ai.lms.core.course.app.port.in.usecase.GetChapterUseCase;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetChapterService implements GetChapterUseCase, GetAllChaptersUseCase {

    private final LoadChapterPort loadChapterPort;
    private final LoadLearningOutcomePort loadLearningOutcomePort;
    private final LoadGroupLearningOutcomePort loadGroupLearningOutcomePort;

    @Override
    @Transactional(readOnly = true)
    public GetChapterResponse getChapter(UUID courseId, UUID chapterId) {
        Chapter chapter = loadChapterPort.findByKey(courseId, chapterId)
                .orElseThrow(() -> new ChapterNotFoundException(
                        "Chapter not found with courseId: " + courseId + " and chapterId: " + chapterId));

        return mapToResponse(chapter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetChapterResponse> getAllChapters(UUID courseId) {
        List<Chapter> chapters = loadChapterPort.findAllByCourseId(courseId);
        return chapters.stream().map(this::mapToResponse).toList();
    }

    private GetChapterResponse mapToResponse(Chapter chapter) {
        Set<UUID> chapterLearningOutcomeIds = Set.copyOf(
                chapter.getLearningOutcomeIds() == null ? List.<UUID>of() : chapter.getLearningOutcomeIds());
        List<LearningOutcome> courseLearningOutcomes = java.util.Optional
                .ofNullable(loadLearningOutcomePort.findByCourseId(chapter.getCourseId()))
                .orElse(List.of());
        List<LearningOutcome> chapterLearningOutcomes = courseLearningOutcomes.stream()
                .filter(lo -> chapterLearningOutcomeIds.contains(lo.getId()))
                .toList();

        List<GroupLearningOutcome> courseGroups = java.util.Optional
                .ofNullable(loadGroupLearningOutcomePort.findByCourseId(chapter.getCourseId()))
                .orElse(List.of());
        Map<UUID, GroupLearningOutcome> groupsById = courseGroups.stream()
                .collect(Collectors.toMap(GroupLearningOutcome::getId, Function.identity()));

        List<GetChapterResponse.LearningOutcomeSummary> ungroupedLearningOutcomes = chapterLearningOutcomes.stream()
                .filter(lo -> lo.getGroupLearningOutcomeId() == null)
                .map(this::toLearningOutcomeSummary)
                .toList();

        Map<UUID, List<LearningOutcome>> groupedChildren = chapterLearningOutcomes.stream()
                .filter(lo -> lo.getGroupLearningOutcomeId() != null)
                .collect(Collectors.groupingBy(LearningOutcome::getGroupLearningOutcomeId));

        List<GetChapterResponse.GroupLearningOutcomeSummary> groupedLearningOutcomes = groupedChildren.entrySet().stream()
                .map(entry -> {
                    GroupLearningOutcome group = groupsById.get(entry.getKey());
                    if (group == null) {
                        return null;
                    }

                    return GetChapterResponse.GroupLearningOutcomeSummary.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .description(group.getDescription())
                            .learningOutcomes(entry.getValue().stream()
                                    .map(this::toLearningOutcomeSummary)
                                    .toList())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return GetChapterResponse.builder()
                .chapterId(chapter.getChapterId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .courseId(chapter.getCourseId())
                .learningOutcomeIds(chapter.getLearningOutcomeIds() == null ? List.of() : chapter.getLearningOutcomeIds())
                .ungroupedLearningOutcomes(ungroupedLearningOutcomes)
                .groupedLearningOutcomes(groupedLearningOutcomes)
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }

    private GetChapterResponse.LearningOutcomeSummary toLearningOutcomeSummary(LearningOutcome learningOutcome) {
        return GetChapterResponse.LearningOutcomeSummary.builder()
                .id(learningOutcome.getId())
                .code(learningOutcome.getCode())
                .description(learningOutcome.getDescription())
                .build();
    }
}
