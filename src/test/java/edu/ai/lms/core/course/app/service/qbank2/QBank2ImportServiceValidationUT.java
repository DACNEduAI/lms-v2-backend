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
    void createsAnUnknownWorkbookCodeAndSkipsDuplicateBlankIdContent() {
        ImportGroup group = group("GROUP", "STACK-IMPORT", "Stack", List.of(
                new ImportExample(null, "Same content", 1), new ImportExample(null, "Same content", 2)));

        ImportPreview preview = QBank2ImportService.validateNewRequest(new ImportRequest(1, "qbank2.xlsx", List.of(group)));

        assertTrue(preview.valid());
        assertEquals(1, preview.summary().createdGroups());
        assertEquals(1, preview.summary().createdExamples());
        assertEquals(1, preview.summary().skippedExamples());
        assertEquals("STACK-IMPORT", preview.groups().getFirst().code());
    }

    @Test
    void validatesTheWorkbookCodeUsingTheExistingSchemaLimit() {
        ImportPreview missing = QBank2ImportService.validateNewRequest(new ImportRequest(1, "qbank2.xlsx",
                List.of(group("GROUP", "", "Stack", List.of()))));
        ImportPreview oversized = QBank2ImportService.validateNewRequest(new ImportRequest(1, "qbank2.xlsx",
                List.of(group("GROUP", "x".repeat(65), "Stack", List.of()))));

        assertFalse(missing.valid());
        assertFalse(oversized.valid());
        assertTrue(missing.issues().stream().anyMatch(issue -> issue.code().equals("GROUP_CODE_REQUIRED")));
        assertTrue(oversized.issues().stream().anyMatch(issue -> issue.code().equals("GROUP_CODE_TOO_LONG")));
    }

    @Test
    void updatesMatchingIdsWithoutDeletingMissingExamplesAndSkipsForeignOrRepeatedIds() {
        UUID first = UUID.randomUUID();
        UUID missingFromTarget = UUID.randomUUID();
        ImportGroup group = group("GROUP", "STACK-IMPORT", "Renamed stack", List.of(
                new ImportExample(first, "Changed", 1),
                new ImportExample(first, "Repeated", 2),
                new ImportExample(missingFromTarget, "Foreign", 3),
                new ImportExample(null, "Existing content", 4),
                new ImportExample(null, "Created", 5),
                new ImportExample(null, "Changed", 6)));

        ImportPreview preview = QBank2ImportService.validateExistingRequest(
                new ImportRequest(1, "qbank2.xlsx", List.of(group)), UUID.randomUUID(), "STACK-IMPORT",
                List.of(new ImportExample(first, "Original", 1), new ImportExample(UUID.randomUUID(), "Existing content", 2)));

        assertTrue(preview.valid());
        assertEquals(1, preview.summary().updatedGroups());
        assertEquals(1, preview.summary().updatedExamples());
        assertEquals(1, preview.summary().createdExamples());
        assertEquals(0, preview.summary().unchangedExamples());
        assertEquals(4, preview.summary().skippedExamples());
    }

    @Test
    void rejectsInvalidWorkbookShapeAndExampleText() {
        ImportPreview preview = QBank2ImportService.validateNewRequest(new ImportRequest(2, "qbank2.csv", List.of(
                new ImportGroup("OTHER", "STACK", "Stack", "Description", List.of(new ImportExample(null, "", 2))))));

        assertFalse(preview.valid());
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("SCHEMA_INVALID")));
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("FILE_INVALID")));
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("KEY_INVALID")));
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("CONTENT_REQUIRED")));
        assertTrue(preview.issues().stream().anyMatch(issue -> issue.code().equals("ORDER_INVALID")));
    }

    private static ImportGroup group(String key, String code, String name, List<ImportExample> examples) {
        return new ImportGroup(key, code, name, "Description", examples);
    }
}
