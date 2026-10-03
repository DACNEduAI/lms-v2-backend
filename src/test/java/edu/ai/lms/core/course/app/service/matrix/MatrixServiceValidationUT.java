package edu.ai.lms.core.course.app.service.matrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;
import edu.ai.lms.core.course.adapter.out.persistence.MatrixAssetCoverageRepository;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;

@ExtendWith(MockitoExtension.class)
class MatrixServiceValidationUT {
    @Mock private JdbcTemplate jdbc;
    @Mock private CourseTeacherAccess access;
    @Mock private LoadSignedInUserPort signedInUserPort;
    @Mock private MatrixAssetCoverageRepository assetCoverages;

    private MatrixService service;
    private UUID courseId;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        service = new MatrixService(jdbc, access, signedInUserPort, assetCoverages);
        courseId = UUID.randomUUID();
        profileId = UUID.randomUUID();
    }

    @Test
    void rejectsPayloadWithoutCcctContentRows() {
        MatrixService.MatrixPayload payload = new MatrixService.MatrixPayload(
                "Empty Matrix", null, MatrixService.MatrixMode.MANUAL, null,
                List.of(), List.of(), List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(courseId, payload));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Matrix needs at least one CCCT content row", error.getReason());
        verifyNoInteractions(jdbc, assetCoverages);
    }

    @Test
    void acceptsPayloadWithOneValidCcctContentRow() {
        UUID levelId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        MatrixService.MatrixPayload payload = new MatrixService.MatrixPayload(
                "Configured Matrix", null, MatrixService.MatrixMode.MANUAL, null,
                List.of(new MatrixService.RowPayload(UUID.randomUUID(), UUID.randomUUID(), outcomeId,
                        List.of(new MatrixService.Cell(levelId, 1)))),
                List.of(new MatrixService.LearningLevelPayload(levelId, "Apply", null)),
                List.of(new MatrixService.OutcomeLearningLevel(outcomeId, levelId)));
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
                .thenAnswer(invocation -> ((String) invocation.getArgument(0)).contains("ccct_profiles") ? profileId : Boolean.TRUE);

        assertDoesNotThrow(() -> service.validate(courseId, payload));
    }
}
