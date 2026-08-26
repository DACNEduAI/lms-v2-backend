package edu.ai.lms.core.course.app.service.qbank2;

import java.util.List;
import java.util.UUID;

public final class QBank2ImportModels {
    private QBank2ImportModels() { }

    public record ImportRequest(int schemaVersion, String fileName, List<ImportGroup> groups) { }
    public record ImportGroup(String importKey, String groupCode, String name, String description,
            List<ImportExample> examples) { }
    public record ImportExample(UUID id, String content, int order) { }
    public record ImportIssue(String sheet, int row, String field, String code, String message) { }
    public record ImportSummary(int createdGroups, int updatedGroups, int createdExamples, int updatedExamples,
            int unchangedExamples, int skippedExamples) { }
    public record GroupPreview(String importKey, String action, String code, int createdExamples, int updatedExamples,
            int unchangedExamples, int skippedExamples) { }
    public record ImportPreview(boolean valid, ImportSummary summary, List<GroupPreview> groups, List<ImportIssue> issues) { }
    public record ImportedGroup(String importKey, UUID id, String code) { }
    public record ImportResult(UUID courseId, String fileName, ImportSummary summary, List<ImportedGroup> groups) { }
}
