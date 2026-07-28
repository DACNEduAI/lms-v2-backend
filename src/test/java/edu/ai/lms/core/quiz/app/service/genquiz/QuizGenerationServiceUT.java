package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationContextPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.QuizGenerationContextView;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ScheduleQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.ValidateMaterialSelectionPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuizGenerationServiceUT {

    @Test
    void snapshotsOneSharedCountForMultipleOutcomes() throws Exception {
        SaveQuizGenerationJobPort savePort = mock(SaveQuizGenerationJobPort.class);
        LoadQuizGenerationJobPort loadPort = mock(LoadQuizGenerationJobPort.class);
        ScheduleQuizGenerationJobPort scheduler = mock(ScheduleQuizGenerationJobPort.class);
        LoadSignedInUserPort userPort = mock(LoadSignedInUserPort.class);
        LoadCourseMembershipPort membershipPort = mock(LoadCourseMembershipPort.class);
        LoadQuizGenerationContextPort contextPort = mock(LoadQuizGenerationContextPort.class);
        ValidateMaterialSelectionPort materialPort = mock(ValidateMaterialSelectionPort.class);
        ObjectMapper objectMapper = new ObjectMapper();
        QuizGenerationService service = new QuizGenerationService(
                savePort, loadPort, scheduler, userPort, membershipPort,
                contextPort, materialPort, objectMapper);

        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID matrixId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID firstOutcomeId = UUID.randomUUID();
        UUID secondOutcomeId = UUID.randomUUID();
        when(userPort.getAuthenticatedUser()).thenReturn(Optional.of(
                CurrentActor.builder().id(teacherId).role(Roles.TEACHER).build()));
        when(membershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(materialPort.findValidationError(courseId, List.of()))
                .thenReturn(Optional.empty());
        when(contextPort.load(courseId, matrixId)).thenReturn(Optional.of(
                new QuizGenerationContextView(
                        courseId, "DSA", matrixId, "Midterm",
                        List.of(new QuizGenerationContextView.Row(
                                rowId, UUID.randomUUID(), "Graphs", "Traversal",
                                List.of(
                                        new QuizGenerationContextView.LearningOutcome(
                                                firstOutcomeId, "LO1", "Explain"),
                                        new QuizGenerationContextView.LearningOutcome(
                                                secondOutcomeId, "LO2", "Apply")),
                                List.of(new QuizGenerationContextView.LearningLevel(
                                        levelId, "SYN", "Synthesize", "Combine",
                                        3, List.of(firstOutcomeId, secondOutcomeId), 4)))))));
        AtomicReference<String> snapshot = new AtomicReference<>();
        when(savePort.save(any())).thenAnswer(invocation -> {
            var job = invocation.getArgument(0,
                    edu.ai.lms.core.quiz.app.domain.QuizGenerationJob.class);
            job.setId(UUID.randomUUID());
            snapshot.set(job.getRequestSnapshot());
            return job;
        });

        service.generateQuiz(GenerateQuizCommand.builder()
                .courseId(courseId).examMatrixId(matrixId)
                .selectedMaterialIds(List.of()).unlimited(true).build());

        JsonNode saved = objectMapper.readTree(snapshot.get());
        assertEquals(2, saved.path("snapshotVersion").asInt());
        assertEquals(4, saved.path("questionCount").asInt());
        assertEquals(1, saved.path("examMatrix").size());
        assertFalse(saved.path("examMatrix").get(0).has("hieu"));
        assertEquals(2, saved.path("examMatrix").get(0)
                .path("levelCounts").get(0).path("learningOutcomeIds").size());
        assertEquals(4, saved.path("examMatrix").get(0)
                .path("levelCounts").get(0).path("questionCount").asInt());
    }
}
