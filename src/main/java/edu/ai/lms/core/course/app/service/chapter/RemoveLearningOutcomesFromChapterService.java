package edu.ai.lms.core.course.app.service.chapter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.RemoveLearningOutcomesFromChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.RemoveLearningOutcomesFromChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherRemoveLearningOutcomesFromChapterUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteChapterLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveLearningOutcomesFromChapterService implements TeacherRemoveLearningOutcomesFromChapterUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadChapterPort loadChapterPort;
    private final DeleteChapterLearningOutcomePort deleteChapterLearningOutcomePort;

    @Override
    @Transactional
    public RemoveLearningOutcomesFromChapterResponse removeLearningOutcomesFromChapter(RemoveLearningOutcomesFromChapterCommand command) {
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify this course");
        }

        Chapter chapter = loadChapterPort.findByKey(command.courseId(), command.chapterId())
                .orElseThrow(() -> new ChapterNotFoundException(
                        "Chapter not found with courseId: " + command.courseId() + " and chapterId: " + command.chapterId()));

        // Validate that all requested LO IDs are currently associated with this chapter
        Set<UUID> currentLOIds = new java.util.HashSet<>(chapter.getLearningOutcomeIds());

        List<UUID> notAssociated = command.learningOutcomeIds().stream()
                .filter(id -> !currentLOIds.contains(id))
                .toList();

        if (!notAssociated.isEmpty()) {
            throw new LearningOutcomeNotFoundException(
                    "Learning outcomes not associated with this chapter: " + notAssociated);
        }

        // Remove LOs from chapter
        for (UUID loId : command.learningOutcomeIds()) {
            deleteChapterLearningOutcomePort.removeLearningOutcomeFromChapter(
                    command.chapterId(), command.courseId(), loId);
        }

        return RemoveLearningOutcomesFromChapterResponse.builder()
                .courseId(command.courseId())
                .chapterId(command.chapterId())
                .removedLearningOutcomeIds(command.learningOutcomeIds())
                .totalRemoved(command.learningOutcomeIds().size())
                .message("Learning outcomes removed from chapter successfully")
                .build();
    }
}
