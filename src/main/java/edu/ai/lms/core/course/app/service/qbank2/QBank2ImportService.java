package edu.ai.lms.core.course.app.service.qbank2;

import static edu.ai.lms.core.course.app.service.qbank2.QBank2ImportModels.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import edu.ai.lms.core.course.app.service.qbank2.QuestionBank2Service.ExampleView;
import edu.ai.lms.core.course.app.service.qbank2.QuestionBank2Service.GroupView;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QBank2ImportService {
    private static final int SCHEMA_VERSION = 1;
    private static final String IMPORT_KEY = "GROUP";
    private static final int MAX_EXAMPLES = 10_000;
    private static final int MAX_NAME = 255;
    private static final int MAX_DESCRIPTION = 5_000;
    private static final int MAX_CONTENT = 50_000;
    private static final long MAX_TOTAL_CONTENT = 10_000_000L;

    private final JdbcTemplate jdbc;
    private final ExamMatrixAccess access;
    private final QuestionBank2Service bank;
    private final QBank2VectorSyncService vectors;

    @Transactional(readOnly = true)
    public ImportPreview previewNew(UUID courseId, ImportRequest request) {
        access.requireCourseTeacher(courseId);
        return validate(courseId, request, ImportMode.NEW);
    }

    @Transactional(readOnly = true)
    public ImportPreview previewExisting(UUID courseId, ImportRequest request) {
        access.requireCourseTeacher(courseId);
        return validate(courseId, request, ImportMode.EXISTING);
    }

    @Transactional
    public ImportResult commitNew(UUID courseId, ImportRequest request) {
        return commit(courseId, request, ImportMode.NEW);
    }

    @Transactional
    public ImportResult commitExisting(UUID courseId, ImportRequest request) {
        return commit(courseId, request, ImportMode.EXISTING);
    }

    private ImportResult commit(UUID courseId, ImportRequest request, ImportMode mode) {
        access.requireCourseTeacher(courseId);
        Map<UUID, GroupState> existingGroups = mode == ImportMode.EXISTING ? loadGroups(courseId) : Map.of();
        ImportPreview preview = validateRequest(request, existingGroups, mode);
        rejectInvalid(preview);

        ImportGroup input = request.groups().getFirst();
        GroupState existing = mode == ImportMode.EXISTING ? existingGroups.get(input.id()) : null;
        UUID groupId;
        String code;
        if (mode == ImportMode.NEW) {
            groupId = UUID.randomUUID();
            int sequence = bank.nextGroupSequence(courseId);
            code = QuestionBank2Service.formatCode(sequence);
            jdbc.update("""
                    INSERT INTO edu.question_bank_2_groups(id,course_id,code,code_sequence,name,description)
                    VALUES (?,?,?,?,?,?)
                    """, groupId, courseId, code, sequence, input.name().trim(), input.description().trim());
        } else {
            groupId = existing.id();
            code = existing.code();
        }

        Set<UUID> modifiedIds = linkedSet(input.modifiedExampleIds());
        Set<UUID> deletedIds = linkedSet(input.deletedExampleIds());
        if (mode == ImportMode.EXISTING) {
            deletedIds.forEach(id -> jdbc.update(
                    "DELETE FROM edu.question_bank_2_examples WHERE id=? AND group_id=?", id, groupId));
            moveModifiedSequences(groupId, modifiedIds);
        }

        Set<UUID> newIds = new LinkedHashSet<>();
        for (ImportExample example : input.examples()) {
            if (example.id() == null) {
                UUID exampleId = UUID.randomUUID();
                newIds.add(exampleId);
                jdbc.update("""
                        INSERT INTO edu.question_bank_2_examples(id,group_id,content,sequence) VALUES (?,?,?,?)
                        """, exampleId, groupId, example.content().trim(), example.order());
            } else if (modifiedIds.contains(example.id())) {
                jdbc.update("""
                        UPDATE edu.question_bank_2_examples SET content=?,sequence=?,updated_at=CURRENT_TIMESTAMP
                        WHERE id=? AND group_id=?
                        """, example.content().trim(), example.order(), example.id(), groupId);
            }
        }

        GroupView finalGroup = loadGroup(courseId, groupId);
        deletedIds.forEach(vectors::delete);
        modifiedIds.forEach(vectors::delete);
        for (ExampleView example : finalGroup.examples()) {
            if (modifiedIds.contains(example.id()) || newIds.contains(example.id())) {
                vectors.upsert(courseId, finalGroup, example);
            }
        }

        ImportedGroup mapping = new ImportedGroup(input.importKey().trim(), groupId, code);
        return new ImportResult(courseId, request.fileName().trim(), preview.summary(), List.of(mapping));
    }

    ImportPreview validate(UUID courseId, ImportRequest request, ImportMode mode) {
        return validateRequest(request, mode == ImportMode.EXISTING ? loadGroups(courseId) : Map.of(), mode);
    }

    static ImportPreview validateNewRequest(ImportRequest request) {
        return validateRequest(request, Map.of(), ImportMode.NEW);
    }

    static ImportPreview validateExistingRequest(ImportRequest request, UUID groupId, String code,
            List<ImportExample> currentExamples) {
        Map<UUID, ExampleState> examples = new LinkedHashMap<>();
        for (ImportExample example : currentExamples) {
            examples.put(example.id(), new ExampleState(example.id(), groupId, trim(example.content()), example.order()));
        }
        GroupState group = new GroupState(groupId, code, "Existing", "Existing", examples);
        return validateRequest(request, Map.of(groupId, group), ImportMode.EXISTING);
    }

    private static ImportPreview validateRequest(ImportRequest request, Map<UUID, GroupState> existingGroups,
            ImportMode mode) {
        List<ImportIssue> issues = new ArrayList<>();
        List<ImportGroup> groups = request == null || request.groups() == null ? List.of() : request.groups();
        if (request == null) add(issues, "Metadata", 0, "request", "REQUEST_INVALID", "Import request is required");
        if (request != null && request.schemaVersion() != SCHEMA_VERSION) {
            add(issues, "Metadata", 2, "schemaVersion", "SCHEMA_INVALID", "Unsupported QBank2 workbook schema version");
        }
        if (request == null || blank(request.fileName()) || request.fileName().trim().length() > 255
                || !request.fileName().toLowerCase().endsWith(".xlsx")) {
            add(issues, "Metadata", 3, "fileName", "FILE_INVALID", "A valid .xlsx file name is required");
        }
        if (groups.size() != 1) {
            add(issues, "Metadata", 0, "group", "GROUP_COUNT", "An import must contain exactly one group");
        }

        int createdGroups = 0;
        int updatedGroups = 0;
        int createdExamples = 0;
        int updatedExamples = 0;
        int deletedExamples = 0;
        int unchangedExamples = 0;
        int totalExamples = 0;
        long totalContent = 0;
        List<GroupPreview> groupPreviews = new ArrayList<>();

        for (ImportGroup group : groups) {
            if (group == null) {
                add(issues, "Metadata", 2, "group", "GROUP_INVALID", "Group data cannot be blank");
                continue;
            }
            String key = trim(group.importKey());
            if (!IMPORT_KEY.equals(key)) {
                add(issues, "Metadata", 2, "importKey", "KEY_INVALID", "The single-group import key is invalid");
            }

            GroupState existing = group.id() == null ? null : existingGroups.get(group.id());
            if (mode == ImportMode.NEW) {
                createdGroups++;
                if (group.id() != null) {
                    add(issues, "Metadata", 2, "id", "IMPORT_MODE_CONFLICT", "New-group imports cannot contain a group ID");
                }
                checkText(issues, "Metadata", 2, "name", group.name(), MAX_NAME);
                checkText(issues, "Metadata", 2, "description", group.description(), MAX_DESCRIPTION);
            } else {
                updatedGroups++;
                if (group.id() == null) {
                    add(issues, "Metadata", 2, "id", "GROUP_ID_CONFLICT", "Existing-group imports require a group ID");
                } else if (existing == null) {
                    add(issues, "Metadata", 2, "id", "GROUP_ID_CONFLICT", "Group ID does not belong to this course");
                }
            }

            List<UUID> providedModified = ids(group.modifiedExampleIds());
            List<UUID> providedDeleted = ids(group.deletedExampleIds());
            Set<UUID> modifiedSet = new LinkedHashSet<>(providedModified);
            Set<UUID> deletedSet = new LinkedHashSet<>(providedDeleted);
            if (modifiedSet.size() != providedModified.size()) {
                add(issues, "Examples", 0, "modifiedExampleIds", "DIFF_CONFLICT", "Modified example IDs contain duplicates");
            }
            if (deletedSet.size() != providedDeleted.size()) {
                add(issues, "Examples", 0, "deletedExampleIds", "DIFF_CONFLICT", "Deleted example IDs contain duplicates");
            }
            if (mode == ImportMode.NEW && (!modifiedSet.isEmpty() || !deletedSet.isEmpty())) {
                add(issues, "Examples", 0, "changes", "IMPORT_MODE_CONFLICT", "New-group imports cannot modify or delete examples");
            }

            List<ImportExample> examples = group.examples() == null ? List.of() : group.examples();
            Set<UUID> seenIds = new HashSet<>();
            Set<UUID> retainedIds = new LinkedHashSet<>();
            Set<UUID> authoritativeModified = new LinkedHashSet<>();
            int groupCreated = 0;
            for (int index = 0; index < examples.size(); index++) {
                ImportExample example = examples.get(index);
                int row = index + 2;
                if (example == null) {
                    add(issues, "Examples", row, "example", "EXAMPLE_INVALID", "Example row cannot be blank");
                    continue;
                }
                checkText(issues, "Examples", row, "content", example.content(), MAX_CONTENT);
                if (example.order() != index + 1) {
                    add(issues, "Examples", row, "order", "ORDER_INVALID", "Example order must be contiguous from 1");
                }
                totalContent += example.content() == null ? 0 : example.content().length();
                if (example.id() == null) {
                    createdExamples++;
                    groupCreated++;
                } else if (mode == ImportMode.NEW) {
                    add(issues, "Examples", row, "id", "IMPORT_MODE_CONFLICT", "New-group examples cannot contain IDs");
                } else {
                    if (!seenIds.add(example.id())) {
                        add(issues, "Examples", row, "id", "EXAMPLE_ID_CONFLICT", "Example ID appears more than once");
                    }
                    retainedIds.add(example.id());
                    ExampleState current = existing == null ? null : existing.examples().get(example.id());
                    if (current == null) {
                        add(issues, "Examples", row, "id", "EXAMPLE_ID_CONFLICT", "Example ID does not belong to the target group");
                    } else if (!trim(example.content()).equals(current.content()) || example.order() != current.sequence()) {
                        authoritativeModified.add(example.id());
                    }
                }
            }

            Set<UUID> authoritativeDeleted = new LinkedHashSet<>();
            if (existing != null) {
                authoritativeDeleted.addAll(existing.examples().keySet());
                authoritativeDeleted.removeAll(retainedIds);
                updatedExamples += authoritativeModified.size();
                deletedExamples += authoritativeDeleted.size();
                unchangedExamples += retainedIds.size() - authoritativeModified.size();
                if (!modifiedSet.equals(authoritativeModified) || !deletedSet.equals(authoritativeDeleted)) {
                    add(issues, "Examples", 0, "changes", "DIFF_CONFLICT",
                            "QBank2 changed or the submitted modified/deleted example sets are incorrect");
                }
            }
            totalExamples += examples.size();
            groupPreviews.add(new GroupPreview(key, mode == ImportMode.NEW ? "CREATE" : "UPDATE",
                    existing == null ? null : existing.code(), groupCreated, authoritativeModified.size(),
                    authoritativeDeleted.size(), retainedIds.size() - authoritativeModified.size()));
        }

        if (totalExamples > MAX_EXAMPLES) {
            add(issues, "Examples", 0, "examples", "EXAMPLE_LIMIT", "A workbook can contain at most " + MAX_EXAMPLES + " examples");
        }
        if (totalContent > MAX_TOTAL_CONTENT) {
            add(issues, "Examples", 0, "content", "CONTENT_LIMIT", "Workbook example content is too large");
        }
        ImportSummary summary = new ImportSummary(createdGroups, updatedGroups, createdExamples, updatedExamples,
                deletedExamples, unchangedExamples);
        return new ImportPreview(issues.isEmpty(), summary, List.copyOf(groupPreviews), List.copyOf(issues));
    }

    private Map<UUID, GroupState> loadGroups(UUID courseId) {
        Map<UUID, GroupState> groups = new LinkedHashMap<>();
        jdbc.query("SELECT id,code,name,description FROM edu.question_bank_2_groups WHERE course_id=?", rs -> {
            UUID id = rs.getObject(1, UUID.class);
            groups.put(id, new GroupState(id, rs.getString(2), rs.getString(3), rs.getString(4), new LinkedHashMap<>()));
        }, courseId);
        jdbc.query("""
                SELECT e.id,e.group_id,e.content,e.sequence FROM edu.question_bank_2_examples e
                JOIN edu.question_bank_2_groups g ON g.id=e.group_id WHERE g.course_id=?
                """, rs -> {
            UUID id = rs.getObject(1, UUID.class);
            UUID groupId = rs.getObject(2, UUID.class);
            GroupState group = groups.get(groupId);
            if (group != null) {
                group.examples().put(id, new ExampleState(id, groupId, rs.getString(3), rs.getInt(4)));
            }
        }, courseId);
        return groups;
    }

    private GroupView loadGroup(UUID courseId, UUID groupId) {
        List<ExampleView> examples = jdbc.query("""
                SELECT id,content,sequence,created_at,updated_at FROM edu.question_bank_2_examples
                WHERE group_id=? ORDER BY sequence,id
                """, (rs, row) -> new ExampleView(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3),
                rs.getObject(4, LocalDateTime.class), rs.getObject(5, LocalDateTime.class)), groupId);
        GroupView group = jdbc.query("""
                SELECT id,code,name,description,code_sequence,created_at,updated_at
                FROM edu.question_bank_2_groups WHERE id=? AND course_id=?
                """, rs -> rs.next() ? new GroupView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getInt(5), examples, rs.getObject(6, LocalDateTime.class),
                rs.getObject(7, LocalDateTime.class)) : null, groupId, courseId);
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "QBank2 group not found");
        }
        return group;
    }

    private void moveModifiedSequences(UUID groupId, Set<UUID> modifiedIds) {
        int offset = 0;
        for (UUID id : modifiedIds) {
            jdbc.update("UPDATE edu.question_bank_2_examples SET sequence=? WHERE id=? AND group_id=?",
                    1_000_000_000 + offset++, id, groupId);
        }
    }

    private static void rejectInvalid(ImportPreview preview) {
        if (preview.valid()) return;
        ImportIssue issue = preview.issues().getFirst();
        HttpStatus status = issue.code().endsWith("_CONFLICT") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        throw new ResponseStatusException(status, issue.message());
    }

    private static List<UUID> ids(List<UUID> values) {
        return values == null ? List.of() : values.stream().filter(java.util.Objects::nonNull).toList();
    }

    private static Set<UUID> linkedSet(List<UUID> values) {
        return new LinkedHashSet<>(ids(values));
    }

    private static void checkText(List<ImportIssue> issues, String sheet, int row, String field, String value, int max) {
        if (blank(value)) {
            add(issues, sheet, row, field, field.toUpperCase() + "_REQUIRED", field + " cannot be blank");
        } else if (value.trim().length() > max) {
            add(issues, sheet, row, field, field.toUpperCase() + "_TOO_LONG", field + " exceeds " + max + " characters");
        }
    }

    private static void add(List<ImportIssue> issues, String sheet, int row, String field, String code, String message) {
        issues.add(new ImportIssue(sheet, row, field, code, message));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    enum ImportMode { NEW, EXISTING }
    private record GroupState(UUID id, String code, String name, String description, Map<UUID, ExampleState> examples) { }
    private record ExampleState(UUID id, UUID groupId, String content, int sequence) { }
}
