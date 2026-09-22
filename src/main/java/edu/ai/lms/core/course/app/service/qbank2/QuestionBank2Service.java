package edu.ai.lms.core.course.app.service.qbank2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import lombok.RequiredArgsConstructor;

/** Course-owned QBank2 content whose committed state is kept in sync with its vector representation. */
@Service
@RequiredArgsConstructor
public class QuestionBank2Service {
    private final JdbcTemplate jdbc;
    private final CourseTeacherAccess access;
    private final QBank2VectorSyncService vectors;

    @Transactional(readOnly = true)
    public QuestionBank2View list(UUID courseId) {
        access.requireCourseTeacher(courseId);
        List<GroupView> groups = jdbc.query("""
                SELECT id,code,name,description,code_sequence,published,created_at,updated_at
                FROM edu.question_bank_2_groups WHERE course_id=? ORDER BY code_sequence,id
                """, (rs, row) -> new GroupView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getInt(5), rs.getBoolean(6), List.of(), rs.getObject(7, LocalDateTime.class),
                rs.getObject(8, LocalDateTime.class)), courseId);
        return new QuestionBank2View(courseId, groups.stream().map(group -> new GroupView(group.id(), group.code(), group.name(),
                group.description(), group.codeSequence(), group.published(), listExamples(group.id()), group.createdAt(), group.updatedAt())).toList());
    }

    @Transactional
    public GroupView createGroup(UUID courseId, GroupInput input) {
        access.requireCourseTeacher(courseId);
        validateGroup(input);
        int sequence = nextGroupSequence(courseId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.question_bank_2_groups(id,course_id,code,code_sequence,name,description) VALUES(?,?,?,?,?,?)",
                id, courseId, formatCode(sequence), sequence, input.name().trim(), input.description().trim());
        return loadGroup(courseId, id);
    }

    @Transactional
    public GroupView updateGroup(UUID courseId, UUID groupId, GroupInput input) {
        access.requireCourseTeacher(courseId);
        validateGroup(input);
        if (jdbc.update("""
                UPDATE edu.question_bank_2_groups SET name=?,description=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=?
                """, input.name().trim(), input.description().trim(), groupId, courseId) != 1) throw notFound("QBank2 group");
        GroupView group = loadGroup(courseId, groupId);
        vectors.upsertAll(courseId, group);
        return group;
    }

    @Transactional
    public GroupView setGroupPublication(UUID courseId, UUID groupId, PublicationInput input) {
        access.requireCourseTeacher(courseId);
        boolean published = requirePublication(input);
        if (jdbc.update("""
                UPDATE edu.question_bank_2_groups SET published=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=?
                """, published, groupId, courseId) != 1) throw notFound("QBank2 group");
        GroupView group = loadGroup(courseId, groupId);
        if (published) vectors.upsertAll(courseId, group);
        else vectors.deleteAll(group.examples());
        return group;
    }

    @Transactional
    public void deleteGroup(UUID courseId, UUID groupId) {
        access.requireCourseTeacher(courseId);
        GroupView group = loadGroup(courseId, groupId);
        if (jdbc.update("DELETE FROM edu.question_bank_2_groups WHERE id=? AND course_id=?", groupId, courseId) != 1) throw notFound("QBank2 group");
        vectors.deleteAll(group.examples());
    }

    @Transactional
    public ExampleView createExample(UUID courseId, UUID groupId, ExampleInput input) {
        access.requireCourseTeacher(courseId);
        validateExample(input);
        GroupView group = loadGroup(courseId, groupId);
        int sequence = jdbc.queryForObject("SELECT COALESCE(MAX(sequence),0)+1 FROM edu.question_bank_2_examples WHERE group_id=?", Integer.class, groupId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.question_bank_2_examples(id,group_id,content,sequence) VALUES(?,?,?,?)",
                id, groupId, input.content().trim(), sequence);
        ExampleView example = loadExample(groupId, id);
        if (effectivelyPublished(group, example)) vectors.upsert(courseId, group, example);
        return example;
    }

    @Transactional
    public ExampleView updateExample(UUID courseId, UUID groupId, UUID exampleId, ExampleInput input) {
        access.requireCourseTeacher(courseId);
        validateExample(input);
        GroupView group = loadGroup(courseId, groupId);
        if (jdbc.update("""
                UPDATE edu.question_bank_2_examples SET content=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND group_id=?
                """, input.content().trim(), exampleId, groupId) != 1) throw notFound("QBank2 example");
        ExampleView example = loadExample(groupId, exampleId);
        vectors.delete(example.id());
        if (effectivelyPublished(group, example)) vectors.upsert(courseId, group, example);
        return example;
    }

    @Transactional
    public ExampleView setExamplePublication(UUID courseId, UUID groupId, UUID exampleId, PublicationInput input) {
        access.requireCourseTeacher(courseId);
        GroupView group = loadGroup(courseId, groupId);
        boolean published = requirePublication(input);
        if (jdbc.update("""
                UPDATE edu.question_bank_2_examples SET published=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND group_id=?
                """, published, exampleId, groupId) != 1) throw notFound("QBank2 example");
        ExampleView example = loadExample(groupId, exampleId);
        if (effectivelyPublished(group, example)) vectors.upsert(courseId, group, example);
        else vectors.delete(example.id());
        return example;
    }

    @Transactional
    public void deleteExample(UUID courseId, UUID groupId, UUID exampleId) {
        access.requireCourseTeacher(courseId);
        requireGroup(courseId, groupId);
        if (jdbc.update("DELETE FROM edu.question_bank_2_examples WHERE id=? AND group_id=?", exampleId, groupId) != 1) throw notFound("QBank2 example");
        vectors.delete(exampleId);
    }

    int nextGroupSequence(UUID courseId) {
        return jdbc.queryForObject("""
                WITH group_max AS (
                    SELECT COALESCE(MAX(code_sequence),0) AS value
                    FROM edu.question_bank_2_groups
                    WHERE course_id=?
                )
                INSERT INTO edu.question_bank_2_code_counters(course_id,next_value)
                SELECT ?, value + 2 FROM group_max
                ON CONFLICT(course_id) DO UPDATE SET next_value=GREATEST(
                    edu.question_bank_2_code_counters.next_value + 1,
                    (SELECT value + 2 FROM group_max)
                )
                RETURNING next_value-1
                """, Integer.class, courseId, courseId);
    }

    private GroupView loadGroup(UUID courseId, UUID groupId) {
        GroupView group = jdbc.query("""
                SELECT id,code,name,description,code_sequence,published,created_at,updated_at
                FROM edu.question_bank_2_groups WHERE id=? AND course_id=?
                """, rs -> rs.next() ? new GroupView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getInt(5), rs.getBoolean(6), listExamples(groupId), rs.getObject(7, LocalDateTime.class),
                rs.getObject(8, LocalDateTime.class)) : null, groupId, courseId);
        if (group == null) throw notFound("QBank2 group");
        return group;
    }

    private void requireGroup(UUID courseId, UUID groupId) { loadGroup(courseId, groupId); }

    private List<ExampleView> listExamples(UUID groupId) {
        return jdbc.query("""
                SELECT id,content,sequence,published,created_at,updated_at FROM edu.question_bank_2_examples
                WHERE group_id=? ORDER BY sequence,id
                """, (rs, row) -> new ExampleView(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3),
                rs.getBoolean(4), rs.getObject(5, LocalDateTime.class), rs.getObject(6, LocalDateTime.class)), groupId);
    }

    private ExampleView loadExample(UUID groupId, UUID exampleId) {
        ExampleView example = jdbc.query("""
                SELECT id,content,sequence,published,created_at,updated_at FROM edu.question_bank_2_examples WHERE id=? AND group_id=?
                """, rs -> rs.next() ? new ExampleView(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3),
                rs.getBoolean(4), rs.getObject(5, LocalDateTime.class), rs.getObject(6, LocalDateTime.class)) : null, exampleId, groupId);
        if (example == null) throw notFound("QBank2 example");
        return example;
    }

    static void validateGroup(GroupInput input) {
        if (input == null || blank(input.name())) throw badRequest("QBank2 group name cannot be blank");
        if (blank(input.description())) throw badRequest("QBank2 group description cannot be blank");
    }

    static void validateExample(ExampleInput input) {
        if (input == null || blank(input.content())) throw badRequest("QBank2 example content cannot be blank");
    }

    static boolean effectivelyPublished(GroupView group, ExampleView example) {
        return group.published() && example.published();
    }

    private static boolean requirePublication(PublicationInput input) {
        if (input == null || input.published() == null) throw badRequest("published is required");
        return input.published();
    }

    static String formatCode(int sequence) { return "QBG-%03d".formatted(sequence); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException notFound(String resource) { return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found"); }

    public record QuestionBank2View(UUID courseId, List<GroupView> groups) { }
    public record GroupView(UUID id, String code, String name, String description, int codeSequence, boolean published, List<ExampleView> examples,
            LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record ExampleView(UUID id, String content, int sequence, boolean published, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record GroupInput(String name, String description) { }
    public record ExampleInput(String content) { }
    public record PublicationInput(Boolean published) { }
}
