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
    void preservesRowAndMapsArbitraryActiveLevelAcrossMultipleOutcomes() {
        UUID courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        ExamMatrixRow row = ExamMatrixRow.builder().id(UUID.randomUUID())
                .chapterId(chapterId).topic("Independent topic")
                .learningOutcomeIds(List.of(first, second))
                .levelCounts(List.of(
                        count(levelId, List.of(first, second), "Synthesize", 3, true),
                        count(UUID.randomUUID(), List.of(first), "Dormant", 9, false),
                        count(UUID.randomUUID(), List.of(second), "Zero", 0, true))).build();
        when(coursePort.findById(courseId)).thenReturn(Optional.of(
                Course.builder().id(courseId).courseName("Course").build()));
        when(matrixPort.findById(matrixId)).thenReturn(Optional.of(
                ExamMatrix.builder().id(matrixId).courseId(courseId).rows(List.of(row))
                        .customLearningLevels(List.of(
                                level(matrixId, levelId, first, "Synthesize"),
                                level(matrixId, UUID.randomUUID(), first, "Analyze"),
                                level(matrixId, UUID.randomUUID(), second, "Evaluate")))
                        .build()));
        when(chapterPort.findByKey(courseId, chapterId)).thenReturn(Optional.of(
                Chapter.builder().chapterId(chapterId).title("Chapter").build()));
        when(outcomePort.findById(first)).thenReturn(Optional.of(outcome(first, courseId)));
        when(outcomePort.findById(second)).thenReturn(Optional.of(outcome(second, courseId)));

        var context = adapter().load(courseId, matrixId).orElseThrow();

        assertEquals(1, context.rows().size());
        var mappedRow = context.rows().getFirst();
        assertEquals("Independent topic", mappedRow.topic());
        assertEquals(List.of(first, second),
                mappedRow.learningOutcomes().stream().map(
                        edu.ai.lms.core.course.api.QuizGenerationContext.LearningOutcomeRequirement::id)
                        .toList());
        assertEquals(1, mappedRow.levelCounts().size());
        assertEquals(levelId, mappedRow.levelCounts().getFirst().customLearningLevelId());
        assertEquals("Synthesize", mappedRow.levelCounts().getFirst().name());
        assertEquals(List.of(first, second),
                mappedRow.levelCounts().getFirst().learningOutcomeIds());
        assertEquals("MULTIPLE_CHOICE", mappedRow.levelCounts().getFirst().questionType());
        assertEquals(3, mappedRow.levelCounts().getFirst().questionCount());
    }

    @Test
    void exposesOnlyCourseOwnedMetadataForTheTutor() {
        UUID courseId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).courseCode("DSA101")
                .courseName("Data Structures").description("Core structures").build();
        LearningOutcome outcome = LearningOutcome.builder().id(outcomeId).courseId(courseId)
                .ktLearningOutcomeId(1L).code("CLO1").description("Explain linked lists").build();
        when(coursePort.findById(courseId)).thenReturn(Optional.of(course));
        when(outcomePort.findByCourseId(courseId)).thenReturn(List.of(outcome));

        var metadata = adapter().findForChat(courseId).orElseThrow();

        assertEquals("DSA101", metadata.code());
        assertEquals("Data Structures", metadata.name());
        assertEquals("Core structures", metadata.description());
        assertEquals(List.of(outcomeId), metadata.learningOutcomes().stream()
                .map(edu.ai.lms.core.course.api.LearningOutcomeView::id).toList());
    }

    private CourseApiAdapter adapter() {
        return new CourseApiAdapter(coursePort, matrixPort, outcomePort, groupPort, chapterPort);
    }

    private LearningOutcome outcome(UUID id, UUID courseId) {
        return LearningOutcome.builder().id(id).courseId(courseId).prefixNumber(1).build();
    }

    private ExamMatrixLevelCount count(
            UUID levelId,
            List<UUID> outcomeIds,
            String name,
            int value,
            boolean active) {
        return ExamMatrixLevelCount.builder().customLearningLevelId(levelId)
                .learningOutcomeIds(outcomeIds).code(name.toUpperCase())
                .name(name).questionType(ExamMatrixQuestionType.MULTIPLE_CHOICE)
                .questionCount(value).active(active).build();
    }

    private CustomLearningLevel level(
            UUID matrixId, UUID levelId, UUID outcomeId, String name) {
        return CustomLearningLevel.builder().id(levelId).matrixId(matrixId)
                .learningOutcomeIds(List.of(outcomeId)).code(name).name(name).build();
    }
}
