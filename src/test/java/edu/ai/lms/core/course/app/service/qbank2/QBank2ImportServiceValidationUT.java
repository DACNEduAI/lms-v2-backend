package edu.ai.lms.core.course.app.service.qbank2;

import static edu.ai.lms.core.course.app.service.qbank2.QBank2ImportModels.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QBank2ImportServiceValidationUT {
    @Test
    void acceptsOneNewGroup() {
        ImportGroup group = new ImportGroup("GROUP", null, "Stack", "Description", List.of(), List.of(), List.of(
                new ImportExample(null, "Same content", 1),
                new ImportExample(null, "Same content", 2)));
        ImportRequest request = new ImportRequest(1, "qbank2.xlsx", List.of(group));

        ImportPreview preview = QBank2ImportService.validateNewRequest(request);

        assertTrue(preview.valid());
        assertEquals(1, preview.summary().createdGroups());
        assertEquals(2, preview.summary().createdExamples());
    }

    @Test
    void rejectsZeroOrMultipleGroups() {
        ImportPreview empty = QBank2ImportService.validateNewRequest(new ImportRequest(1, "qbank2.xlsx", List.of()));
        ImportPreview multiple = QBank2ImportService.validateNewRequest(new ImportRequest(1, "qbank2.xlsx",
                List.of(group("GROUP", "Stack", "Question"), group("GROUP", "Queue", "Question"))));

        assertFalse(empty.valid());
        assertFalse(multiple.valid());
        assertTrue(empty.issues().stream().anyMatch(issue -> issue.code().equals("GROUP_COUNT")));
        assertTrue(multiple.issues().stream().anyMatch(issue -> issue.code().equals("GROUP_COUNT")));
    }

    @Test
    void rejectsInvalidInternalKeyAndNonContiguousOrder() {
        ImportGroup group = new ImportGroup("STACK", null, "Stack", "Description", List.of(), List.of(),
                List.of(new ImportExample(null, "Question", 2)));
        ImportPreview preview = QBank2ImportService.validateNewRequest(new ImportRequest(1, "qbank2.xlsx", List.of(group)));

        assertFalse(preview.valid());
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("KEY_INVALID")));
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("ORDER_INVALID")));
    }

    @Test
    void rejectsUnsupportedSchemaAndNonExcelFileName() {
        ImportPreview preview = QBank2ImportService.validateNewRequest(new ImportRequest(2, "qbank2.csv",
                List.of(group("GROUP", "Stack", "Question"))));

        assertFalse(preview.valid());
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("SCHEMA_INVALID")));
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("FILE_INVALID")));
    }

    @Test
    void verifiesExistingExampleDiffIncludingSequenceChanges() {
        UUID groupId = UUID.randomUUID();
        UUID unchangedId = UUID.randomUUID();
        UUID movedId = UUID.randomUUID();
        UUID deletedId = UUID.randomUUID();
        List<ImportExample> current = List.of(
                new ImportExample(unchangedId, "Unchanged", 1),
                new ImportExample(deletedId, "Deleted", 2),
                new ImportExample(movedId, "Moved", 3));
        ImportGroup group = new ImportGroup("GROUP", groupId, "Ignored", "Ignored", List.of(movedId),
                List.of(deletedId), List.of(
                        new ImportExample(unchangedId, "Unchanged", 1),
                        new ImportExample(movedId, "Moved", 2),
                        new ImportExample(null, "Created", 3)));

        ImportPreview preview = QBank2ImportService.validateExistingRequest(
                new ImportRequest(1, "qbank2.xlsx", List.of(group)), groupId, "QBG-001", current);

        assertTrue(preview.valid());
        assertEquals(1, preview.summary().createdExamples());
        assertEquals(1, preview.summary().updatedExamples());
        assertEquals(1, preview.summary().deletedExamples());
        assertEquals(1, preview.summary().unchangedExamples());
    }

    @Test
    void rejectsIncorrectExistingExampleDiff() {
        UUID groupId = UUID.randomUUID();
        UUID exampleId = UUID.randomUUID();
        ImportGroup group = new ImportGroup("GROUP", groupId, "Ignored", "Ignored", List.of(), List.of(),
                List.of(new ImportExample(exampleId, "Changed", 1)));

        ImportPreview preview = QBank2ImportService.validateExistingRequest(
                new ImportRequest(1, "qbank2.xlsx", List.of(group)), groupId, "QBG-001",
                List.of(new ImportExample(exampleId, "Original", 1)));

        assertFalse(preview.valid());
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("DIFF_CONFLICT")));
    }

    private static ImportGroup group(String key, String name, String content) {
        return new ImportGroup(key, null, name, "Description", List.of(), List.of(),
                List.of(new ImportExample(null, content, 1)));
    }
}
