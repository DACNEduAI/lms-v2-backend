package edu.ai.lms.core.course.app.service.customlearninglevel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.CustomLearningLevel;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CustomLearningLevelValidationException;
import edu.ai.lms.core.course.app.port.in.command.BatchEditCustomLearningLevelsCommand;
import edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue;
import edu.ai.lms.core.course.app.port.out.CustomLearningLevelPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;

@ExtendWith(MockitoExtension.class)
class CustomLearningLevelServiceUT {
    @Mock ExamMatrixAccess access;
    @Mock CustomLearningLevelPort port;
    @InjectMocks CustomLearningLevelService service;

    UUID courseId;
    UUID outcomeId;
    UUID secondOutcomeId;
    Course course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        outcomeId = UUID.randomUUID();
        secondOutcomeId = UUID.randomUUID();
        course = Course.builder().id(courseId)
                .learningOutcomes(List.of(
                        LearningOutcome.builder().id(outcomeId).courseId(courseId).build(),
                        LearningOutcome.builder().id(secondOutcomeId).courseId(courseId).build()))
                .build();
        lenient().when(access.requireCourseTeacher(courseId)).thenReturn(course);
    }

    @Test
    void createsTrimmedLevelForCourseOutcome() {
        var value = new CustomLearningLevelValue(
                null, null, List.of(outcomeId), " CLL1 ", " Understand ", " Explain ", 0);
        when(port.lockAllByCourseId(courseId)).thenReturn(List.of());
        when(port.findByCourseIdAndId(eq(courseId), any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(1);
            return Optional.of(CustomLearningLevel.builder().id(id).courseId(courseId)
                    .learningOutcomeIds(List.of(outcomeId))
                    .code("CLL1").name("Understand").description("Explain").sortOrder(0).build());
        });

        var response = service.create(courseId, value);

        assertEquals("CLL1", response.code());
        verify(port).insert(argThat(level -> level.getCode().equals("CLL1")
                && level.getName().equals("Understand")));
    }

    @Test
    void rejectsCaseInsensitiveDuplicateCodeWithinOutcome() {
        when(port.lockAllByCourseId(courseId)).thenReturn(List.of(level("CLL1", "Understand")));
        var value = new CustomLearningLevelValue(
                null, null, List.of(outcomeId), "cll1", "Different", null, 1);

        assertThrows(CustomLearningLevelValidationException.class,
                () -> service.create(courseId, value));
        verify(port, never()).insert(any());
    }

    @Test
    void appliesMixedBatchAndReturnsClientMapping() {
        CustomLearningLevel existing = level("CLL1", "Understand");
        when(port.lockAllByCourseId(courseId)).thenReturn(List.of(existing));
        when(port.findAllByCourseId(courseId)).thenReturn(List.of());
        var command = new BatchEditCustomLearningLevelsCommand(courseId,
                List.of(new CustomLearningLevelValue(
                        null, "temp-1", List.of(outcomeId), "CLL2", "Apply", null, 1)),
                List.of(new CustomLearningLevelValue(
                        existing.getId(), null, List.of(outcomeId), "CLL1A", "Understand", null, 0)),
                List.of());

        var response = service.batchEdit(command);

        assertEquals("temp-1", response.createdLevelMappings().getFirst().clientTempId());
        verify(port).stageUpdate(eq(existing.getId()), startsWith("__tmp__"));
        verify(port).insert(any());
        verify(port).update(argThat(level -> level.getCode().equals("CLL1A")));
    }

    @Test
    void rejectsDeleteWhenLevelIsUsedWithoutMutating() {
        CustomLearningLevel existing = level("CLL1", "Understand");
        when(port.lockAllByCourseId(courseId)).thenReturn(List.of(existing));
        when(port.countMatrixUsages(existing.getId())).thenReturn(2L);
        var command = new BatchEditCustomLearningLevelsCommand(
                courseId, List.of(), List.of(), List.of(existing.getId()));

        assertThrows(CustomLearningLevelValidationException.class,
                () -> service.batchEdit(command));
        verify(port, never()).delete(any());
    }

    @Test
    void replacesLearningOutcomeMappingsWhenTeacherTogglesThem() {
        CustomLearningLevel existing = level("CLL1", "Understand");
        when(port.lockAllByCourseId(courseId)).thenReturn(List.of(existing));
        when(port.findAllByCourseId(courseId)).thenReturn(List.of());
        var command = new BatchEditCustomLearningLevelsCommand(
                courseId,
                List.of(),
                List.of(new CustomLearningLevelValue(
                        existing.getId(), null, List.of(secondOutcomeId),
                        "CLL1", "Understand", null, 0)),
                List.of());

        service.batchEdit(command);

        verify(port).update(argThat(level ->
                level.getLearningOutcomeIds().equals(List.of(secondOutcomeId))));
    }

    @Test
    void rejectsRemovingMappingsNeededByExistingMatrixCounts() {
        CustomLearningLevel existing = level("CLL1", "Understand");
        when(port.lockAllByCourseId(courseId)).thenReturn(List.of(existing));
        when(port.countIncompatibleMatrixUsages(
                existing.getId(), List.of(secondOutcomeId))).thenReturn(1L);
        var command = new BatchEditCustomLearningLevelsCommand(
                courseId,
                List.of(),
                List.of(new CustomLearningLevelValue(
                        existing.getId(), null, List.of(secondOutcomeId),
                        "CLL1", "Understand", null, 0)),
                List.of());

        assertThrows(CustomLearningLevelValidationException.class,
                () -> service.batchEdit(command));
        verify(port, never()).update(any());
    }

    @Test
    void rejectsEmptyAndOverlappingBatches() {
        assertThrows(CustomLearningLevelValidationException.class,
                () -> service.batchEdit(new BatchEditCustomLearningLevelsCommand(
                        courseId, List.of(), List.of(), List.of())));

        UUID id = UUID.randomUUID();
        assertThrows(CustomLearningLevelValidationException.class,
                () -> service.batchEdit(new BatchEditCustomLearningLevelsCommand(
                        courseId,
                        List.of(),
                        List.of(new CustomLearningLevelValue(
                                id, null, List.of(outcomeId), "C", "N", null, 0)),
                        List.of(id))));
    }

    private CustomLearningLevel level(String code, String name) {
        return CustomLearningLevel.builder()
                .id(UUID.randomUUID()).courseId(courseId)
                .learningOutcomeIds(List.of(outcomeId))
                .code(code).name(name).sortOrder(0).build();
    }
}
