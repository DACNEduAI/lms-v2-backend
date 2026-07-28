package edu.ai.lms.core.course.adapter.in.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.port.out.*;

@ExtendWith(MockitoExtension.class)
class CourseApiAdapterExamMatrixUT {
    @Mock LoadCoursePort coursePort;
    @Mock LoadExamMatrixPort matrixPort;
    @Mock LoadLearningOutcomePort outcomePort;
    @Mock LoadGroupLearningOutcomePort groupPort;
    @Mock LoadChapterPort chapterPort;

    @Test
    void splitsRowsByOutcomeAndMapsLegacyLevelNames() {
        UUID courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        ExamMatrixRow row = ExamMatrixRow.builder().id(UUID.randomUUID()).chapterId(chapterId)
                .learningOutcomeIds(List.of(first, second))
                .levelCounts(List.of(
                        count(first, "Understand", 2),
                        count(second, "Apply", 3))).build();
        when(coursePort.findById(courseId)).thenReturn(Optional.of(
                Course.builder().id(courseId).courseName("Course").build()));
        when(matrixPort.findById(matrixId)).thenReturn(Optional.of(
                ExamMatrix.builder().id(matrixId).courseId(courseId).rows(List.of(row))
                        .customLearningLevels(List.of(
                                level(matrixId, first, "Understand"),
                                level(matrixId, first, "Apply"),
                                level(matrixId, second, "Apply Plus")))
                        .build()));
        when(chapterPort.findByKey(courseId, chapterId)).thenReturn(Optional.of(
                Chapter.builder().chapterId(chapterId).title("Chapter").build()));
        when(outcomePort.findById(first)).thenReturn(Optional.of(outcome(first, courseId)));
        when(outcomePort.findById(second)).thenReturn(Optional.of(outcome(second, courseId)));

        var context = adapter().load(courseId, matrixId).orElseThrow();

        assertEquals(2, context.rows().size());
        assertEquals(2, context.rows().getFirst().understandCount());
        assertEquals(3, context.rows().getLast().applyCount());
    }

    @Test
    void rejectsPositiveUnsupportedCustomLevelForQuizGeneration() {
        UUID courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        ExamMatrixRow row = ExamMatrixRow.builder()
                .levelCounts(List.of(count(UUID.randomUUID(), "Analyze", 1))).build();
        when(coursePort.findById(courseId)).thenReturn(Optional.of(Course.builder().id(courseId).build()));
        when(matrixPort.findById(matrixId)).thenReturn(Optional.of(
                ExamMatrix.builder().id(matrixId).courseId(courseId).rows(List.of(row)).build()));
        assertThrows(IllegalStateException.class, () -> adapter().load(courseId, matrixId));
    }

    private CourseApiAdapter adapter() {
        return new CourseApiAdapter(coursePort, matrixPort, outcomePort, groupPort, chapterPort);
    }

    private LearningOutcome outcome(UUID id, UUID courseId) {
        return LearningOutcome.builder().id(id).courseId(courseId).prefixNumber(1).build();
    }

    private ExamMatrixLevelCount count(UUID outcomeId, String name, int value) {
        return ExamMatrixLevelCount.builder().learningOutcomeIds(List.of(outcomeId))
                .name(name).questionCount(value).build();
    }

    private CustomLearningLevel level(UUID matrixId, UUID outcomeId, String name) {
        return CustomLearningLevel.builder().id(UUID.randomUUID()).matrixId(matrixId)
                .learningOutcomeIds(List.of(outcomeId)).code(name).name(name).build();
    }
}
