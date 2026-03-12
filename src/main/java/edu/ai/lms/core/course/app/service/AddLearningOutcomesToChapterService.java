package edu.ai.lms.core.course.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.course.adapter.in.response.AddLearningOutcomesToChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.AddLearningOutcomesToChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherAddLearningOutcomesToChapterUseCase;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddLearningOutcomesToChapterService implements TeacherAddLearningOutcomesToChapterUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadChapterPort loadChapterPort;
    private final SaveChapterLearningOutcomePort saveChapterLearningOutcomePort;

    @Override
    @Transactional
    public AddLearningOutcomesToChapterResponse addLearningOutcomesToChapter(AddLearningOutcomesToChapterCommand command) {
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        Chapter chapter = loadChapterPort.findByKey(command.courseId(), command.chapterNumber())
                .orElseThrow(() -> new ChapterNotFoundException(
                        "Chapter not found with courseId: " + command.courseId() + " and chapterNumber: " + command.chapterNumber()));

        // Validate that all requested LO IDs belong to this course
        Set<UUID> courseLOIds = course.getLearningOutcomes().stream()
                .map(LearningOutcome::getId)
                .collect(Collectors.toSet());

        List<UUID> invalidIds = command.learningOutcomeIds().stream()
                .filter(id -> !courseLOIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new LearningOutcomeNotFoundException(
                    "Learning outcomes not found in this course: " + invalidIds);
        }

        // Add LOs to chapter, skipping duplicates
        Set<UUID> currentLOIds = new java.util.HashSet<>(chapter.getLearningOutcomeIds());
        List<UUID> actuallyAdded = new ArrayList<>();

        for (UUID loId : command.learningOutcomeIds()) {
            if (!currentLOIds.contains(loId)) {
                saveChapterLearningOutcomePort.addLearningOutcomeToChapter(
                        command.chapterNumber(), command.courseId(), loId);
                actuallyAdded.add(loId);
            }
        }

        return AddLearningOutcomesToChapterResponse.builder()
                .courseId(command.courseId())
                .chapterNumber(command.chapterNumber())
                .addedLearningOutcomeIds(actuallyAdded)
                .totalAdded(actuallyAdded.size())
                .message("Learning outcomes added to chapter successfully")
                .build();
    }
}
