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
                SELECT g.course_id,g.id,g.code,g.name,g.description,e.id,e.content,e.sequence,e.created_at,e.updated_at
                FROM edu.question_bank_2_groups g JOIN edu.question_bank_2_examples e ON e.group_id=g.id
                ORDER BY g.course_id,g.code_sequence,e.sequence,e.id
                """, (rs, row) -> new Row(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getObject(6, UUID.class), rs.getString(7), rs.getInt(8),
                rs.getObject(9, LocalDateTime.class), rs.getObject(10, LocalDateTime.class)));
        try {
            for (Row row : rows) vectors.upsert(row.courseId(),
                    new QuestionBank2Service.GroupView(row.groupId(), row.groupCode(), row.groupName(), row.groupDescription(), 0,
                            List.of(), null, null),
                    new QuestionBank2Service.ExampleView(row.exampleId(), row.exampleContent(), row.sequence(), row.createdAt(), row.updatedAt()));
            complete.set(true);
            log.info("event=qbank2.vector.bootstrap.completed example_count={}", rows.size());
        } catch (Exception ex) {
            log.warn("event=qbank2.vector.bootstrap.retry example_count={} error_type={}", rows.size(), ex.getClass().getSimpleName());
        }
    }

    private record Row(UUID courseId, UUID groupId, String groupCode, String groupName, String groupDescription,
            UUID exampleId, String exampleContent, int sequence, LocalDateTime createdAt, LocalDateTime updatedAt) { }
}
