package edu.ai.lms.core.course.app.service.matrix3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class Matrix3ServiceValidationUT {
    private final Matrix3Service service = new Matrix3Service(null, null, null);

    @Test
    void requiresAMatrixOwnedTotalAndOneSelectedRelation() {
        UUID row = UUID.randomUUID(), level = UUID.randomUUID(), chapter = UUID.randomUUID(), outcome = UUID.randomUUID();
        var valid = new Matrix3Service.Matrix3Detail(UUID.randomUUID(), UUID.randomUUID(), "Matrix 3", null, 15,
                Matrix3Service.LifecycleStatus.DRAFT, 0, null, null,
                List.of(new Matrix3Service.Matrix3Row(row, chapter, "Chapter", outcome, "LO", 0,
                        List.of(new Matrix3Service.Matrix3CellRelation(level, true)))),
                List.of(new Matrix3Service.Matrix3LearningLevel(level, "CLL", 0)), null, null);
        assertDoesNotThrow(() -> service.validateConfirmable(valid));
        var noTotal = new Matrix3Service.Matrix3Detail(valid.id(), valid.courseId(), valid.title(), null, 0,
                valid.lifecycleStatus(), valid.version(), null, null, valid.rows(), valid.customLearningLevels(), null, null);
        assertThrows(ResponseStatusException.class, () -> service.validateConfirmable(noTotal));
    }
}
