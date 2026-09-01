package edu.ai.lms.core.course.app.service.qbank2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Retries seed/backfill indexing without making backend startup depend on AI availability. */
@Component
@RequiredArgsConstructor
@Slf4j
class QBank2VectorBootstrap {
    private final JdbcTemplate jdbc;
    private final QBank2VectorSyncService vectors;
    private final AtomicBoolean complete = new AtomicBoolean(false);

    @Scheduled(initialDelayString = "${qbank2.vector.bootstrap-initial-delay-ms:5000}", fixedDelayString = "${qbank2.vector.bootstrap-delay-ms:30000}")
    void syncUntilComplete() {
        if (complete.get()) return;
        List<Row> rows = jdbc.query("""
                SELECT g.course_id,g.id,g.code,g.name,g.description,g.published,e.id,e.content,e.sequence,e.published,e.created_at,e.updated_at
                FROM edu.question_bank_2_groups g JOIN edu.question_bank_2_examples e ON e.group_id=g.id
                ORDER BY g.course_id,g.code_sequence,e.sequence,e.id
                """, (rs, row) -> new Row(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getBoolean(6), rs.getObject(7, UUID.class), rs.getString(8), rs.getInt(9),
                rs.getBoolean(10), rs.getObject(11, LocalDateTime.class), rs.getObject(12, LocalDateTime.class)));
        try {
            for (Row row : rows) {
                QuestionBank2Service.GroupView group = new QuestionBank2Service.GroupView(row.groupId(), row.groupCode(),
                        row.groupName(), row.groupDescription(), 0, row.groupPublished(), List.of(), null, null);
                QuestionBank2Service.ExampleView example = new QuestionBank2Service.ExampleView(row.exampleId(),
                        row.exampleContent(), row.sequence(), row.examplePublished(), row.createdAt(), row.updatedAt());
                if (QuestionBank2Service.effectivelyPublished(group, example)) vectors.upsert(row.courseId(), group, example);
                else vectors.delete(row.exampleId());
            }
            complete.set(true);
            log.info("event=qbank2.vector.bootstrap.completed example_count={}", rows.size());
        } catch (Exception ex) {
            log.warn("event=qbank2.vector.bootstrap.retry example_count={} error_type={}", rows.size(), ex.getClass().getSimpleName());
        }
    }

    private record Row(UUID courseId, UUID groupId, String groupCode, String groupName, String groupDescription,
            boolean groupPublished, UUID exampleId, String exampleContent, int sequence, boolean examplePublished,
            LocalDateTime createdAt, LocalDateTime updatedAt) { }
}
