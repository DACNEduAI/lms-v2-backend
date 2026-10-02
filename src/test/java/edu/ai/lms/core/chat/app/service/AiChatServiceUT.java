package edu.ai.lms.core.chat.app.service;

import edu.ai.lms.core.chat.adapter.in.http.request.PythonChatRequest;
import edu.ai.lms.core.chat.app.port.out.ValidateChatMaterialsPort;
import edu.ai.lms.core.chat.app.port.out.persistence.AiChatPersistencePort;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import edu.ai.lms.core.learnermodel.app.domain.LearnerState;
import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcome;
import edu.ai.lms.core.learnermodel.app.domain.LearnerStateOutcomeStatus;
import edu.ai.lms.core.learnermodel.app.domain.PredictionStatus;
import edu.ai.lms.core.learnermodel.app.port.in.usecase.GetLatestLearnerStateUseCase;
import edu.ai.lms.core.material.api.MaterialSelectionApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatServiceUT {
    private final GetLatestLearnerStateUseCase latestState = mock(GetLatestLearnerStateUseCase.class);
    private final LearningOutcomeCatalogApi outcomes = mock(LearningOutcomeCatalogApi.class);
    private final AiChatService service = new AiChatService(
            mock(WebClient.class), mock(AiChatPersistencePort.class), mock(ValidateChatMaterialsPort.class),
            mock(edu.ai.lms.core.course.api.CourseChatMetadataApi.class), mock(MaterialSelectionApi.class),
            latestState, outcomes);

    @Test
    void buildsRagProfileFromAktPyktInsteadOfAggregateProbability() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        LearnerState state = new LearnerState(UUID.randomUUID(), UUID.randomUUID(), courseId, UUID.randomUUID(),
                LocalDateTime.now(), List.of(new LearnerStateOutcome(outcomeId, 0.50,
                LearnerStateOutcomeStatus.PREDICTED, 0.80, PredictionStatus.PREDICTED)));
        when(latestState.getLatest(courseId)).thenReturn(Optional.of(state));
        when(outcomes.findByIds(courseId, List.of(outcomeId))).thenReturn(List.of(
                new LearningOutcomeView(outcomeId, 1, "CLO1", 1, "Linked lists", null, null, null)));

        PythonChatRequest.LearnerState payload = learnerState(courseId);

        assertThat(payload.getOutcomes()).singleElement().satisfies(outcome -> {
            assertThat(outcome.getProbability()).isEqualTo(0.80);
            assertThat(outcome.getStatus()).isEqualTo("PREDICTED");
        });
    }

    @Test
    void omitsRagProfileForLegacyAggregateOnlySnapshot() throws Exception {
        UUID courseId = UUID.randomUUID();
        LearnerState state = new LearnerState(UUID.randomUUID(), UUID.randomUUID(), courseId, UUID.randomUUID(),
                LocalDateTime.now(), List.of(new LearnerStateOutcome(UUID.randomUUID(), 0.50,
                LearnerStateOutcomeStatus.PREDICTED, null, null)));
        when(latestState.getLatest(courseId)).thenReturn(Optional.of(state));

        assertThat(learnerState(courseId)).isNull();
    }

    @Test
    void forwardsUnavailableAktPyktValueAndStatusForNewSnapshot() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        LearnerState state = new LearnerState(UUID.randomUUID(), UUID.randomUUID(), courseId, UUID.randomUUID(),
                LocalDateTime.now(), List.of(new LearnerStateOutcome(outcomeId, null,
                LearnerStateOutcomeStatus.INSUFFICIENT_PREDICTION, null,
                PredictionStatus.INSUFFICIENT_OUTCOME_HISTORY)));
        when(latestState.getLatest(courseId)).thenReturn(Optional.of(state));
        when(outcomes.findByIds(courseId, List.of(outcomeId))).thenReturn(List.of(
                new LearningOutcomeView(outcomeId, 1, "CLO1", 1, "Linked lists", null, null, null)));

        PythonChatRequest.LearnerState payload = learnerState(courseId);

        assertThat(payload.getOutcomes()).singleElement().satisfies(outcome -> {
            assertThat(outcome.getProbability()).isNull();
            assertThat(outcome.getStatus()).isEqualTo("INSUFFICIENT_OUTCOME_HISTORY");
        });
    }

    private PythonChatRequest.LearnerState learnerState(UUID courseId) throws Exception {
        Method method = AiChatService.class.getDeclaredMethod("learnerState", UUID.class);
        method.setAccessible(true);
        return (PythonChatRequest.LearnerState) method.invoke(service, courseId);
    }
}
