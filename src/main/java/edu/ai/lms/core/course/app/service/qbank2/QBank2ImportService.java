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
    private static final int MAX_GROUP_CODE = 64;
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
    public ImportPreview preview(UUID courseId, ImportRequest request) {
        access.requireCourseTeacher(courseId);
        return evaluate(request, loadGroups(courseId)).preview();
    }

    @Transactional
    public ImportResult commit(UUID courseId, ImportRequest request) {
        access.requireCourseTeacher(courseId);
        Evaluation evaluation = evaluate(request, loadGroups(courseId));
        rejectInvalid(evaluation.preview());

        GroupPlan plan = evaluation.plan();
        ImportGroup input = plan.input();
        GroupState existing = plan.existing();
        UUID groupId;
        String code = trim(input.groupCode());
        int nextSequence = 1;
        if (existing == null) {
            groupId = UUID.randomUUID();
            int codeSequence = bank.nextGroupSequence(courseId);
            jdbc.update("""
                    INSERT INTO edu.question_bank_2_groups(id,course_id,code,code_sequence,name,description)
                    VALUES (?,?,?,?,?,?)
                    """, groupId, courseId, code, codeSequence, trim(input.name()), trim(input.description()));
        } else {
            groupId = existing.id();
            nextSequence = existing.examples().values().stream().mapToInt(ExampleState::sequence).max().orElse(0) + 1;
            jdbc.update("""
                    UPDATE edu.question_bank_2_groups SET name=?,description=?,updated_at=CURRENT_TIMESTAMP
                    WHERE id=? AND course_id=?
                    """, trim(input.name()), trim(input.description()), groupId, courseId);
        }

        Set<UUID> updatedIds = new LinkedHashSet<>();
        Set<UUID> createdIds = new LinkedHashSet<>();
        for (PlannedExample planned : plan.examples()) {
            ImportExample example = planned.input();
            if (planned.action() == ExampleAction.CREATE) {
                UUID exampleId = UUID.randomUUID();
                createdIds.add(exampleId);
                jdbc.update("""
                        INSERT INTO edu.question_bank_2_examples(id,group_id,content,sequence) VALUES (?,?,?,?)
                        """, exampleId, groupId, trim(example.content()), nextSequence++);
            } else if (planned.action() == ExampleAction.UPDATE) {
                updatedIds.add(example.id());
                jdbc.update("""
                        UPDATE edu.question_bank_2_examples SET content=?,updated_at=CURRENT_TIMESTAMP
                        WHERE id=? AND group_id=?
                        """, trim(example.content()), example.id(), groupId);
            }
        }

        GroupView finalGroup = loadGroup(courseId, groupId);
        updatedIds.forEach(vectors::delete);
        for (ExampleView example : finalGroup.examples()) {
            if (updatedIds.contains(example.id()) || createdIds.contains(example.id())) {
                vectors.upsert(courseId, finalGroup, example);
            }
        }

        ImportedGroup mapping = new ImportedGroup(trim(input.importKey()), groupId, code);
        return new ImportResult(courseId, trim(request.fileName()), evaluation.preview().summary(), List.of(mapping));
    }

    static ImportPreview validateNewRequest(ImportRequest request) {
        return evaluate(request, Map.of()).preview();
    }

    static ImportPreview validateExistingRequest(ImportRequest request, UUID groupId, String code,
            List<ImportExample> currentExamples) {
        Map<UUID, ExampleState> examples = new LinkedHashMap<>();
        for (ImportExample example : currentExamples) {
            examples.put(example.id(), new ExampleState(example.id(), groupId, trim(example.content()), example.order()));
        }
        GroupState group = new GroupState(groupId, code, "Existing", "Existing", examples);
        return evaluate(request, Map.of(code, group)).preview();
    }

    private static Evaluation evaluate(ImportRequest request, Map<String, GroupState> groupsByCode) {
        List<ImportIssue> issues = new ArrayList<>();
        List<ImportGroup> groups = request == null || request.groups() == null ? List.of() : request.groups();
        if (request == null) add(issues, "Metadata", 0, "request", "REQUEST_INVALID", "Import request is required");
        if (request != null && request.schemaVersion() != SCHEMA_VERSION) {
            add(issues, "Metadata", 2, "schemaVersion", "SCHEMA_INVALID", "Unsupported QBank2 workbook schema version");
        }
        if (request == null || blank(request.fileName()) || request.fileName().trim().length() > 255
                || !request.fileName().toLowerCase().endsWith(".xlsx")) {
            add(issues, "Metadata", 2, "fileName", "FILE_INVALID", "A valid .xlsx file name is required");
        }
        if (groups.size() != 1) {
            add(issues, "Metadata", 0, "group", "GROUP_COUNT", "An import must contain exactly one group");
        }
        if (groups.isEmpty() || groups.getFirst() == null) {
            ImportSummary summary = new ImportSummary(0, 0, 0, 0, 0, 0);
            return new Evaluation(new ImportPreview(issues.isEmpty(), summary, List.of(), List.copyOf(issues)), null);
        }

        ImportGroup input = groups.getFirst();
        String key = trim(input.importKey());
        String code = trim(input.groupCode());
        if (!IMPORT_KEY.equals(key)) add(issues, "Metadata", 2, "importKey", "KEY_INVALID", "The single-group import key is invalid");
        if (blank(code)) add(issues, "Metadata", 3, "groupCode", "GROUP_CODE_REQUIRED", "groupCode cannot be blank");
        else if (code.length() > MAX_GROUP_CODE) add(issues, "Metadata", 3, "groupCode", "GROUP_CODE_TOO_LONG", "groupCode exceeds " + MAX_GROUP_CODE + " characters");
        checkText(issues, "Metadata", 4, "name", input.name(), MAX_NAME);
        checkText(issues, "Metadata", 5, "description", input.description(), MAX_DESCRIPTION);

        GroupState existing = groupsByCode.get(code);
        List<ImportExample> examples = input.examples() == null ? List.of() : input.examples();
        Set<UUID> seenIds = new HashSet<>();
        Set<String> seenContent = new HashSet<>();
        if (existing != null) existing.examples().values().forEach(example -> seenContent.add(example.content()));
        List<PlannedExample> planned = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;
        long totalContent = 0;

        for (int index = 0; index < examples.size(); index++) {
            ImportExample example = examples.get(index);
            int row = index + 2;
            if (example == null) {
                add(issues, "Examples", row, "example", "EXAMPLE_INVALID", "Example row cannot be blank");
                continue;
            }
            checkText(issues, "Examples", row, "content", example.content(), MAX_CONTENT);
            if (example.order() != index + 1) add(issues, "Examples", row, "order", "ORDER_INVALID", "Example order must be contiguous from 1");
            totalContent += example.content() == null ? 0 : example.content().length();
            if (blank(example.content()) || trim(example.content()).length() > MAX_CONTENT) continue;

            ExampleAction action;
            if (example.id() != null) {
                if (!seenIds.add(example.id())) {
                    action = ExampleAction.SKIP;
                } else {
                    ExampleState current = existing == null ? null : existing.examples().get(example.id());
                    if (current == null) {
                        action = ExampleAction.SKIP;
                    } else if (trim(example.content()).equals(current.content())) {
                        action = ExampleAction.UNCHANGED;
                    } else {
                        action = ExampleAction.UPDATE;
                    }
                }
            } else if (!seenContent.add(trim(example.content()))) {
                action = ExampleAction.SKIP;
            } else {
                action = ExampleAction.CREATE;
            }
            if (action != ExampleAction.SKIP) seenContent.add(trim(example.content()));
            planned.add(new PlannedExample(example, action));
            switch (action) {
                case CREATE -> created++;
                case UPDATE -> updated++;
                case UNCHANGED -> unchanged++;
                case SKIP -> skipped++;
            }
        }

        if (examples.size() > MAX_EXAMPLES) {
            add(issues, "Examples", 0, "examples", "EXAMPLE_LIMIT", "A workbook can contain at most " + MAX_EXAMPLES + " examples");
        }
        if (totalContent > MAX_TOTAL_CONTENT) {
            add(issues, "Examples", 0, "content", "CONTENT_LIMIT", "Workbook example content is too large");
        }

        ImportSummary summary = new ImportSummary(existing == null ? 1 : 0, existing == null ? 0 : 1, created, updated,
                unchanged, skipped);
        GroupPreview groupPreview = new GroupPreview(key, existing == null ? "CREATE" : "UPDATE", code, created, updated,
                unchanged, skipped);
        return new Evaluation(new ImportPreview(issues.isEmpty(), summary, List.of(groupPreview), List.copyOf(issues)),
                new GroupPlan(input, existing, List.copyOf(planned)));
    }

    private Map<String, GroupState> loadGroups(UUID courseId) {
        Map<String, GroupState> groups = new LinkedHashMap<>();
        Map<UUID, GroupState> groupsById = new LinkedHashMap<>();
        jdbc.query("SELECT id,code,name,description FROM edu.question_bank_2_groups WHERE course_id=?", rs -> {
            UUID id = rs.getObject(1, UUID.class);
            GroupState group = new GroupState(id, rs.getString(2), rs.getString(3), rs.getString(4), new LinkedHashMap<>());
            groups.put(group.code(), group);
            groupsById.put(group.id(), group);
        }, courseId);
        jdbc.query("""
                SELECT e.id,e.group_id,e.content,e.sequence FROM edu.question_bank_2_examples e
                JOIN edu.question_bank_2_groups g ON g.id=e.group_id WHERE g.course_id=?
                """, rs -> {
            UUID id = rs.getObject(1, UUID.class);
            UUID groupId = rs.getObject(2, UUID.class);
            GroupState group = groupsById.get(groupId);
            if (group != null) group.examples().put(id, new ExampleState(id, groupId, rs.getString(3), rs.getInt(4)));
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
        if (group == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "QBank2 group not found");
        return group;
    }

    private static void rejectInvalid(ImportPreview preview) {
        if (preview.valid()) return;
        ImportIssue issue = preview.issues().getFirst();
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, issue.message());
    }

    private static void checkText(List<ImportIssue> issues, String sheet, int row, String field, String value, int max) {
        if (blank(value)) add(issues, sheet, row, field, field.toUpperCase() + "_REQUIRED", field + " cannot be blank");
        else if (value.trim().length() > max) add(issues, sheet, row, field, field.toUpperCase() + "_TOO_LONG", field + " exceeds " + max + " characters");
    }

    private static void add(List<ImportIssue> issues, String sheet, int row, String field, String code, String message) {
        issues.add(new ImportIssue(sheet, row, field, code, message));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String trim(String value) { return value == null ? "" : value.trim(); }

    private enum ExampleAction { CREATE, UPDATE, UNCHANGED, SKIP }
    private record Evaluation(ImportPreview preview, GroupPlan plan) { }
    private record GroupPlan(ImportGroup input, GroupState existing, List<PlannedExample> examples) { }
    private record PlannedExample(ImportExample input, ExampleAction action) { }
    private record GroupState(UUID id, String code, String name, String description, Map<UUID, ExampleState> examples) { }
    private record ExampleState(UUID id, UUID groupId, String content, int sequence) { }
}
