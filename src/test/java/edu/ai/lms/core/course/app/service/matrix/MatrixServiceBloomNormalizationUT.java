package edu.ai.lms.core.course.app.service.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatrixServiceBloomNormalizationUT {
    @Test
    void derivesMissingAssignmentFromAnArbitraryLegacyCll() {
        UUID rowId=UUID.randomUUID(), itemId=UUID.randomUUID(), outcomeId=UUID.randomUUID(), legacyLevelId=UUID.randomUUID();
        MatrixService.MatrixPayload normalized=MatrixService.withDefaultOutcomeLearningLevels(new MatrixService.MatrixPayload(
                "Legacy draft",null,MatrixService.MatrixMode.MANUAL,null,
                List.of(new MatrixService.RowPayload(rowId,itemId,outcomeId,List.of(new MatrixService.Cell(legacyLevelId,2)))),
                List.of(new MatrixService.LearningLevelPayload(legacyLevelId,"Legacy skill",List.of())),List.of()));

        assertEquals(List.of(new MatrixService.OutcomeLearningLevel(outcomeId,legacyLevelId)),normalized.outcomeLearningLevels());
        assertEquals(legacyLevelId,normalized.rows().getFirst().cells().getFirst().customLearningLevelId());
    }

    @Test
    void leavesAmbiguousLegacyColumnsUnassigned() {
        UUID firstRowId=UUID.randomUUID(), secondRowId=UUID.randomUUID(), firstItemId=UUID.randomUUID(), secondItemId=UUID.randomUUID(), outcomeId=UUID.randomUUID(), firstLevelId=UUID.randomUUID(), secondLevelId=UUID.randomUUID();
        MatrixService.MatrixPayload normalized=MatrixService.withDefaultOutcomeLearningLevels(new MatrixService.MatrixPayload(
                "Legacy draft",null,MatrixService.MatrixMode.MANUAL,null,
                List.of(new MatrixService.RowPayload(firstRowId,firstItemId,outcomeId,List.of(new MatrixService.Cell(firstLevelId,1))),new MatrixService.RowPayload(secondRowId,secondItemId,outcomeId,List.of(new MatrixService.Cell(secondLevelId,1)))),
                List.of(new MatrixService.LearningLevelPayload(firstLevelId,"Foundations",List.of()),new MatrixService.LearningLevelPayload(secondLevelId,"Advanced application",List.of())),List.of()));

        assertEquals(List.of(),normalized.outcomeLearningLevels());
    }
}
