package edu.ai.lms.core.quiz.adapter.out.persistence;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.GenerationMode;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.LifecycleStatus;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.SourceLifecycleStatus;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.SourceOption;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AssessmentView create(
            UUID courseId,
            UUID actorId,
            String title,
            String description,
            Integer timeLimitMinutes,
            Double passScore,
            LocalDateTime startTime,
            LocalDateTime dueTime,
            Integer attemptLimited,
            GenerationMode generationMode) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.assessments
                    (id, course_id, title, description, unlimited, time_limit_minutes, pass_score,
                     start_time, due_time, attempt_limited, variant_count, option_count,
                     generation_mode, allow_recently_used_questions, created_by, current_step)
                VALUES
                    (:id, :courseId, :title, :description, false, :timeLimitMinutes, :passScore,
                     :startTime, :dueTime, :attemptLimited, NULL, NULL, :generationMode, false, :actorId, :currentStep)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("courseId", courseId)
                .addValue("title", title)
                .addValue("description", description)
                .addValue("timeLimitMinutes", timeLimitMinutes)
                .addValue("passScore", passScore)
                .addValue("startTime", startTime)
                .addValue("dueTime", dueTime)
                .addValue("attemptLimited", attemptLimited)
                .addValue("generationMode", generationMode.name())
                .addValue("currentStep", generationMode == GenerationMode.QUICK_TEST ? 5 : 1)
                .addValue("actorId", actorId));
        return get(courseId, id).orElseThrow();
    }

    public List<AssessmentView> list(UUID courseId) {
        return jdbc.query(assessmentSelect() + " WHERE a.course_id = :courseId ORDER BY a.updated_at DESC, a.id",
                Map.of("courseId", courseId), this::mapAssessment);
    }

    public Optional<AssessmentView> get(UUID courseId, UUID assessmentId) {
        return jdbc.query(assessmentSelect() + " WHERE a.course_id = :courseId AND a.id = :assessmentId",
                Map.of("courseId", courseId, "assessmentId", assessmentId), this::mapAssessment)
                .stream().findFirst();
    }

    public Optional<AssessmentView> getForUpdate(UUID courseId, UUID assessmentId) {
        return jdbc.query(assessmentSelect() + " WHERE a.course_id = :courseId AND a.id = :assessmentId FOR UPDATE",
                Map.of("courseId", courseId, "assessmentId", assessmentId), this::mapAssessment)
                .stream().findFirst();
    }

    public boolean update(
            UUID courseId,
            UUID assessmentId,
            long expectedVersion,
            String title,
            String description,
            Integer timeLimitMinutes,
            Double passScore,
            LocalDateTime startTime,
            LocalDateTime dueTime,
            Integer attemptLimited) {
        int updated = jdbc.update("""
                UPDATE edu.assessments
                SET title = :title,
                    description = :description,
                    unlimited = false,
                    time_limit_minutes = :timeLimitMinutes,
                    pass_score = :passScore,
                    start_time = :startTime,
                    due_time = :dueTime,
                    attempt_limited = :attemptLimited,
                    allow_recently_used_questions = false,
                    recent_use_override_by = NULL,
                    recent_use_override_at = NULL,
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId AND version = :expectedVersion
                  AND lifecycle_status IN ('DRAFT', 'READY_TO_PUBLISH')
                """, new MapSqlParameterSource()
                .addValue("assessmentId", assessmentId)
                .addValue("courseId", courseId)
                .addValue("expectedVersion", expectedVersion)
                .addValue("title", title)
                .addValue("description", description)
                .addValue("timeLimitMinutes", timeLimitMinutes)
                .addValue("passScore", passScore)
                .addValue("startTime", startTime)
                .addValue("dueTime", dueTime)
                .addValue("attemptLimited", attemptLimited));
        return updated == 1;
    }

    public UUID baseGenerationJobId(UUID courseId, UUID assessmentId) {
        return jdbc.query("""
                SELECT base_generation_job_id FROM edu.assessments
                WHERE id = :assessmentId AND course_id = :courseId
                """, Map.of("assessmentId", assessmentId, "courseId", courseId),
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null);
    }

    public List<MatrixCellRef> matrixCells(UUID matrixId) {
        return jdbc.query("""
                SELECT DISTINCT exam_matrix_row_id, custom_learning_level_id
                FROM edu.exam_matrix_row_learning_level_counts
                WHERE question_count > 0 AND exam_matrix_row_id IN
                    (SELECT id FROM edu.exam_matrix_rows WHERE matrix_id = :matrixId)
                ORDER BY exam_matrix_row_id, custom_learning_level_id
                """, Map.of("matrixId", matrixId), (rs, rowNum) -> new MatrixCellRef(
                rs.getObject("exam_matrix_row_id", UUID.class),
                rs.getObject("custom_learning_level_id", UUID.class)));
    }

    public void startBaseGeneration(UUID courseId, UUID assessmentId, UUID jobId, int optionCount) {
        int updated = jdbc.update("""
                UPDATE edu.assessments
                SET option_count = :optionCount, base_generation_job_id = :jobId,
                    ai_generation_started = true, lifecycle_status = 'GENERATING', current_step = 5,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                  AND current_step = 5 AND base_generation_job_id IS NULL
                """, Map.of("optionCount", optionCount, "jobId", jobId,
                "assessmentId", assessmentId, "courseId", courseId));
        if (updated != 1) throw new IllegalStateException("Base Quiz generation already started or Assessment changed");
    }

    public void startQuickTestGeneration(UUID courseId, UUID assessmentId, UUID jobId, int optionCount) {
        int updated = jdbc.update("""
                UPDATE edu.assessments
                SET option_count = :optionCount, base_generation_job_id = :jobId,
                    ai_generation_started = true, lifecycle_status = 'GENERATING', current_step = 5,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                  AND generation_mode = 'QUICK_TEST' AND current_step = 5 AND base_generation_job_id IS NULL
                """, Map.of("optionCount", optionCount, "jobId", jobId,
                "assessmentId", assessmentId, "courseId", courseId));
        if (updated != 1) throw new IllegalStateException("Quick Test generation already started or Assessment changed");
    }

    @Transactional
    public void discardQuickTest(UUID courseId, UUID assessmentId) {
        UUID quizId = jdbc.query("""
                SELECT v.quiz_id FROM edu.assessment_variants v
                JOIN edu.assessments a ON a.id = v.assessment_id
                WHERE a.id = :assessmentId AND a.course_id = :courseId
                  AND a.generation_mode = 'QUICK_TEST' AND v.variant_kind = 'BASE'
                """, Map.of("assessmentId", assessmentId, "courseId", courseId),
                rs -> rs.next() ? rs.getObject("quiz_id", UUID.class) : null);
        jdbc.update("DELETE FROM edu.assessment_variants WHERE assessment_id = :assessmentId", Map.of("assessmentId", assessmentId));
        if (quizId != null) jdbc.update("DELETE FROM edu.quizzes WHERE id = :quizId", Map.of("quizId", quizId));
        int updated = jdbc.update("""
                UPDATE edu.assessments
                SET generation_mode = 'STANDARD', base_generation_job_id = NULL, option_count = NULL,
                    ai_generation_started = false, lifecycle_status = 'DRAFT', current_step = 2,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                  AND generation_mode = 'QUICK_TEST' AND current_step = 6
                """, Map.of("assessmentId", assessmentId, "courseId", courseId));
        if (updated != 1) throw new IllegalStateException("Quick Test Assessment was not found");
    }

    public void resetFailedBaseGeneration(UUID courseId, UUID assessmentId, UUID jobId) {
        jdbc.update("""
                UPDATE edu.assessments
                SET base_generation_job_id = NULL, option_count = NULL, ai_generation_started = false,
                    lifecycle_status = 'DRAFT', current_step = 5,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId AND base_generation_job_id = :jobId
                """, Map.of("assessmentId", assessmentId, "courseId", courseId, "jobId", jobId));
    }

    public List<SourceOption> listCcctProfiles(UUID courseId) {
        return jdbc.query("""
                SELECT id, name AS title, status, version, updated_at
                FROM edu.ccct_profiles
                WHERE course_id = :courseId
                ORDER BY updated_at DESC, id
                """, Map.of("courseId", courseId), (rs, rowNum) -> new SourceOption(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                SourceLifecycleStatus.valueOf(rs.getString("status")),
                rs.getLong("version"),
                "DRAFT".equals(rs.getString("status")),
                rs.getObject("updated_at", LocalDateTime.class)));
    }

    public List<SourceOption> listExamMatrices(UUID courseId) {
        return jdbc.query("""
                SELECT id, title, lifecycle_status AS status, version, updated_at
                FROM edu.exam_matrices
                WHERE course_id = :courseId
                ORDER BY updated_at DESC, id
                """, Map.of("courseId", courseId), (rs, rowNum) -> new SourceOption(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                SourceLifecycleStatus.valueOf(rs.getString("status")),
                rs.getLong("version"),
                "DRAFT".equals(rs.getString("status")),
                rs.getObject("updated_at", LocalDateTime.class)));
    }

    @Transactional
    public UUID cloneCcct(UUID courseId, UUID sourceId, UUID actorId) {
        Map<String, Object> source = jdbc.queryForMap("""
                SELECT name, template_title, format_version, structured_current, last_mode, markdown_content
                FROM edu.ccct_profiles WHERE course_id = :courseId AND id = :sourceId
                """, Map.of("courseId", courseId, "sourceId", sourceId));
        String name = copyName(source.get("name"));
        UUID cloneId = UUID.randomUUID();
        MapSqlParameterSource profile = new MapSqlParameterSource(source)
                .addValue("id", cloneId).addValue("courseId", courseId)
                .addValue("name", name).addValue("actorId", actorId);
        jdbc.update("""
                INSERT INTO edu.ccct_profiles
                    (id, course_id, name, template_title, status, version, format_version,
                     structured_current, last_mode, markdown_content, created_by)
                VALUES (:id, :courseId, :name, :template_title, 'DRAFT', 0, :format_version,
                        :structured_current, :last_mode, :markdown_content, :actorId)
                """, profile);

        Map<UUID, UUID> chapterIds = new HashMap<>();
        jdbc.query("""
                SELECT id, course_chapter_id, title, custom_content, order_index
                FROM edu.ccct_chapters WHERE profile_id = :sourceId ORDER BY order_index
                """, Map.of("sourceId", sourceId), rs -> {
            UUID oldId = rs.getObject("id", UUID.class);
            UUID newId = UUID.randomUUID();
            chapterIds.put(oldId, newId);
            jdbc.update("""
                    INSERT INTO edu.ccct_chapters
                        (id, profile_id, course_chapter_id, title, custom_content, order_index)
                    VALUES (:id, :profileId, :courseChapterId, :title, :customContent, :orderIndex)
                    """, new MapSqlParameterSource()
                    .addValue("id", newId).addValue("profileId", cloneId)
                    .addValue("courseChapterId", rs.getObject("course_chapter_id", UUID.class))
                    .addValue("title", rs.getString("title"))
                    .addValue("customContent", rs.getString("custom_content"))
                    .addValue("orderIndex", rs.getInt("order_index")));
        });

        Map<UUID, UUID> topicIds = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : chapterIds.entrySet()) {
            jdbc.query("""
                    SELECT id, title, order_index FROM edu.ccct_topics
                    WHERE chapter_id = :chapterId ORDER BY order_index
                    """, Map.of("chapterId", entry.getKey()), rs -> {
                UUID oldId = rs.getObject("id", UUID.class);
                UUID newId = UUID.randomUUID();
                topicIds.put(oldId, newId);
                jdbc.update("""
                        INSERT INTO edu.ccct_topics (id, chapter_id, title, order_index)
                        VALUES (:id, :chapterId, :title, :orderIndex)
                        """, new MapSqlParameterSource().addValue("id", newId)
                        .addValue("chapterId", entry.getValue()).addValue("title", rs.getString("title"))
                        .addValue("orderIndex", rs.getInt("order_index")));
            });
        }
        for (Map.Entry<UUID, UUID> entry : topicIds.entrySet()) {
            jdbc.query("""
                    SELECT content, coverage_status, note, order_index FROM edu.ccct_items
                    WHERE topic_id = :topicId ORDER BY order_index
                    """, Map.of("topicId", entry.getKey()), rs -> {
                jdbc.update("""
                        INSERT INTO edu.ccct_items (id, topic_id, content, coverage_status, note, order_index)
                        VALUES (:id, :topicId, :content, :coverageStatus, :note, :orderIndex)
                        """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                        .addValue("topicId", entry.getValue()).addValue("content", rs.getString("content"))
                        .addValue("coverageStatus", rs.getString("coverage_status"))
                        .addValue("note", rs.getString("note")).addValue("orderIndex", rs.getInt("order_index")));
            });
        }
        return cloneId;
    }

    @Transactional
    public UUID cloneMatrix(UUID courseId, UUID sourceId) {
        Map<String, Object> source = jdbc.queryForMap("""
                SELECT title, description FROM edu.exam_matrices WHERE course_id = :courseId AND id = :sourceId
                """, Map.of("courseId", courseId, "sourceId", sourceId));
        String title = copyName(source.get("title"));
        UUID cloneId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.exam_matrices (id, course_id, title, description, lifecycle_status, version)
                VALUES (:id, :courseId, :title, :description, 'DRAFT', 0)
                """, new MapSqlParameterSource(source).addValue("id", cloneId)
                .addValue("courseId", courseId).addValue("title", title));

        Map<UUID, UUID> levelIds = new HashMap<>();
        jdbc.query("""
                SELECT id, code, name, description, sort_order FROM edu.custom_learning_levels
                WHERE matrix_id = :sourceId ORDER BY sort_order, id
                """, Map.of("sourceId", sourceId), rs -> {
            UUID oldId = rs.getObject("id", UUID.class);
            UUID newId = UUID.randomUUID();
            levelIds.put(oldId, newId);
            jdbc.update("""
                    INSERT INTO edu.custom_learning_levels
                        (id, matrix_id, code, name, description, sort_order)
                    VALUES (:id, :matrixId, :code, :name, :description, :sortOrder)
                    """, new MapSqlParameterSource().addValue("id", newId).addValue("matrixId", cloneId)
                    .addValue("code", rs.getString("code")).addValue("name", rs.getString("name"))
                    .addValue("description", rs.getString("description")).addValue("sortOrder", rs.getInt("sort_order")));
            jdbc.update("""
                    INSERT INTO edu.custom_learning_level_learning_outcomes
                        (custom_learning_level_id, learning_outcome_id, is_selected)
                    SELECT :newId, learning_outcome_id, is_selected
                    FROM edu.custom_learning_level_learning_outcomes
                    WHERE custom_learning_level_id = :oldId
                    """, Map.of("newId", newId, "oldId", oldId));
        });

        Map<UUID, UUID> rowIds = new HashMap<>();
        jdbc.query("""
                SELECT id, chapter_id, topic, order_index FROM edu.exam_matrix_rows
                WHERE matrix_id = :sourceId ORDER BY order_index, id
                """, Map.of("sourceId", sourceId), rs -> {
            UUID oldId = rs.getObject("id", UUID.class);
            UUID newId = UUID.randomUUID();
            rowIds.put(oldId, newId);
            jdbc.update("""
                    INSERT INTO edu.exam_matrix_rows (id, matrix_id, chapter_id, topic, order_index)
                    VALUES (:id, :matrixId, :chapterId, :topic, :orderIndex)
                    """, new MapSqlParameterSource().addValue("id", newId).addValue("matrixId", cloneId)
                    .addValue("chapterId", rs.getObject("chapter_id", UUID.class))
                    .addValue("topic", rs.getString("topic")).addValue("orderIndex", rs.getInt("order_index")));
            jdbc.update("""
                    INSERT INTO edu.exam_matrix_row_learning_outcomes
                        (exam_matrix_row_id, learning_outcome_id, is_selected)
                    SELECT :newId, learning_outcome_id, is_selected
                    FROM edu.exam_matrix_row_learning_outcomes WHERE exam_matrix_row_id = :oldId
                    """, Map.of("newId", newId, "oldId", oldId));
        });
        for (Map.Entry<UUID, UUID> row : rowIds.entrySet()) {
            jdbc.query("""
                    SELECT custom_learning_level_id, question_type, question_count
                    FROM edu.exam_matrix_row_learning_level_counts
                    WHERE exam_matrix_row_id = :rowId
                    """, Map.of("rowId", row.getKey()), rs -> {
                UUID oldLevelId = rs.getObject("custom_learning_level_id", UUID.class);
                UUID newLevelId = levelIds.get(oldLevelId);
                jdbc.update("""
                        INSERT INTO edu.exam_matrix_row_learning_level_counts
                            (exam_matrix_row_id, custom_learning_level_id, matrix_id, question_type, question_count)
                        VALUES (:rowId, :levelId, :matrixId, :questionType, :questionCount)
                        """, new MapSqlParameterSource().addValue("rowId", row.getValue())
                        .addValue("levelId", newLevelId).addValue("matrixId", cloneId)
                        .addValue("questionType", rs.getString("question_type"))
                        .addValue("questionCount", rs.getInt("question_count")));
            });
        }
        return cloneId;
    }

    @Transactional
    public UUID createCcctDraft(UUID courseId, String name, UUID actorId) {
        List<Map<String, Object>> chapters = jdbc.queryForList("""
                SELECT chapter_id, title FROM edu.chapters
                WHERE course_id = :courseId ORDER BY chapter_id
                """, Map.of("courseId", courseId));
        if (chapters.isEmpty()) {
            throw new IllegalStateException("Create at least one Course Chapter before creating a CCCT Draft");
        }
        UUID profileId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.ccct_profiles
                    (id, course_id, name, template_title, status, version, format_version,
                     structured_current, last_mode, markdown_content, created_by)
                VALUES (:id, :courseId, :name, 'Course Content Coverage Template', 'DRAFT', 0, 2,
                        false, 'CUSTOM', '', :actorId)
                """, new MapSqlParameterSource().addValue("id", profileId)
                .addValue("courseId", courseId).addValue("name", name).addValue("actorId", actorId));
        for (int index = 0; index < chapters.size(); index++) {
            Map<String, Object> chapter = chapters.get(index);
            jdbc.update("""
                    INSERT INTO edu.ccct_chapters
                        (id, profile_id, course_chapter_id, title, custom_content, order_index)
                    VALUES (:id, :profileId, :courseChapterId, :title, '', :orderIndex)
                    """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                    .addValue("profileId", profileId).addValue("courseChapterId", chapter.get("chapter_id"))
                    .addValue("title", chapter.get("title")).addValue("orderIndex", index));
        }
        return profileId;
    }

    @Transactional
    public UUID createMatrixDraft(UUID courseId, String title) {
        List<UUID> outcomeIds = jdbc.query("""
                SELECT id FROM edu.learning_outcomes
                WHERE course_id = :courseId ORDER BY sort_order, id
                """, Map.of("courseId", courseId), (rs, rowNum) -> rs.getObject("id", UUID.class));
        if (outcomeIds.isEmpty()) {
            throw new IllegalStateException("Create at least one Learning Outcome before creating an Exam Matrix Draft");
        }
        UUID matrixId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.exam_matrices
                    (id, course_id, title, description, lifecycle_status, version)
                VALUES (:id, :courseId, :title, NULL, 'DRAFT', 0)
                """, Map.of("id", matrixId, "courseId", courseId, "title", title));
        String[] names = {"Understand", "Apply", "Advanced apply"};
        for (int index = 0; index < names.length; index++) {
            UUID levelId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO edu.custom_learning_levels
                        (id, matrix_id, code, name, description, sort_order)
                    VALUES (:id, :matrixId, :code, :name, 'Define this custom learning level.', :sortOrder)
                    """, new MapSqlParameterSource().addValue("id", levelId).addValue("matrixId", matrixId)
                    .addValue("code", "CLL" + (index + 1)).addValue("name", names[index])
                    .addValue("sortOrder", index));
            for (UUID outcomeId : outcomeIds) {
                jdbc.update("""
                        INSERT INTO edu.custom_learning_level_learning_outcomes
                            (custom_learning_level_id, learning_outcome_id, is_selected)
                        VALUES (:levelId, :outcomeId, true)
                        """, Map.of("levelId", levelId, "outcomeId", outcomeId));
            }
        }
        return matrixId;
    }

    static String copyName(Object sourceName) {
        return String.valueOf(sourceName) + "-Copy";
    }

    public void selectSources(UUID courseId, UUID assessmentId, UUID ccctProfileId, UUID matrixId) {
        int updated = jdbc.update("""
                UPDATE edu.assessments a
                SET ccct_profile_id = :ccctProfileId,
                    ccct_profile_version = (SELECT version FROM edu.ccct_profiles
                        WHERE id = :ccctProfileId AND course_id = :courseId),
                    exam_matrix_id = :matrixId,
                    exam_matrix_version = (SELECT version FROM edu.exam_matrices
                        WHERE id = :matrixId AND course_id = :courseId),
                    current_step = GREATEST(current_step, 2),
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE a.id = :assessmentId AND a.course_id = :courseId
                  AND EXISTS (SELECT 1 FROM edu.ccct_profiles c WHERE c.id = :ccctProfileId AND c.course_id = :courseId)
                  AND EXISTS (SELECT 1 FROM edu.exam_matrices m WHERE m.id = :matrixId AND m.course_id = :courseId)
                  AND a.ai_generation_started = false
                """, Map.of("courseId", courseId, "assessmentId", assessmentId,
                "ccctProfileId", ccctProfileId, "matrixId", matrixId));
        if (updated != 1) {
            throw new IllegalStateException("CCCT or Exam Matrix is invalid for this assessment");
        }
    }

    @Transactional
    public void publishCcct(UUID courseId, UUID assessmentId, UUID profileId, long version, UUID actorId) {
        Map<String, Object> current = jdbc.queryForMap("""
                SELECT status, version FROM edu.ccct_profiles WHERE course_id = :courseId AND id = :profileId FOR UPDATE
                """, Map.of("courseId", courseId, "profileId", profileId));
        String status = String.valueOf(current.get("status"));
        long actualVersion = ((Number) current.get("version")).longValue();
        if ("PUBLISHED".equals(status)) {
            updateSourceProgress(courseId, assessmentId, 4, profileId, actualVersion, true, false, null);
            return;
        }
        if (!"DRAFT".equals(status) || actualVersion != version) {
            throw new IllegalStateException("CCCT changed before publish; reload and review it again");
        }
        Integer examItems = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.ccct_items i
                JOIN edu.ccct_topics t ON t.id = i.topic_id
                JOIN edu.ccct_chapters c ON c.id = t.chapter_id
                WHERE c.profile_id = :profileId AND i.coverage_status = 'LEARN_AND_EXAM'
                """, Map.of("profileId", profileId), Integer.class);
        if (examItems == null || examItems == 0) {
            throw new IllegalStateException("CCCT must contain at least one LEARN_AND_EXAM item");
        }
        UUID previous = jdbc.query("""
                SELECT id FROM edu.ccct_profiles WHERE course_id = :courseId AND status = 'PUBLISHED' FOR UPDATE
                """, Map.of("courseId", courseId), (rs, rowNum) -> rs.getObject("id", UUID.class))
                .stream().findFirst().orElse(null);
        if (previous != null && !previous.equals(profileId)) {
            jdbc.update("UPDATE edu.ccct_profiles SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                    Map.of("id", previous));
        }
        int published = jdbc.update("""
                UPDATE edu.ccct_profiles
                SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :profileId AND course_id = :courseId AND status = 'DRAFT' AND version = :version
                """, Map.of("profileId", profileId, "courseId", courseId, "version", version));
        if (published != 1) throw new IllegalStateException("CCCT publish conflicted with another update");
        updateSourceProgress(courseId, assessmentId, 4, profileId, version + 1, true, true, previous);
    }

    @Transactional
    public void publishMatrix(UUID courseId, UUID assessmentId, UUID matrixId, long version, UUID actorId) {
        Integer total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(c.question_count), 0)
                FROM edu.exam_matrix_row_learning_level_counts c
                JOIN edu.exam_matrix_rows r ON r.id = c.exam_matrix_row_id
                WHERE r.matrix_id = :matrixId
                """, Map.of("matrixId", matrixId), Integer.class);
        if (total == null || total <= 0) {
            throw new IllegalStateException("Exam Matrix must require at least one question");
        }
        String status = jdbc.queryForObject("""
                SELECT lifecycle_status FROM edu.exam_matrices
                WHERE id = :matrixId AND course_id = :courseId
                """, Map.of("matrixId", matrixId, "courseId", courseId), String.class);
        if ("PUBLISHED".equals(status)) {
            long actualVersion = jdbc.queryForObject("SELECT version FROM edu.exam_matrices WHERE id = :matrixId",
                    Map.of("matrixId", matrixId), Long.class);
            updateMatrixProgress(courseId, assessmentId, matrixId, actualVersion, false);
            return;
        }
        int published = jdbc.update("""
                UPDATE edu.exam_matrices
                SET lifecycle_status = 'PUBLISHED', version = version + 1,
                    published_by = :actorId, published_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = :matrixId AND course_id = :courseId
                  AND lifecycle_status = 'DRAFT' AND version = :version
                """, Map.of("actorId", actorId, "matrixId", matrixId, "courseId", courseId, "version", version));
        if (published != 1) throw new IllegalStateException("Exam Matrix changed before publish; reload and review it again");
        updateMatrixProgress(courseId, assessmentId, matrixId, version + 1, true);
    }

    @Transactional
    public void rollbackSources(UUID courseId, UUID assessmentId) {
        Map<String, Object> assessment = jdbc.queryForMap("""
                SELECT ccct_profile_id, previous_published_ccct_id, ccct_published_by_assessment,
                       exam_matrix_id, matrix_published_by_assessment, ai_generation_started
                FROM edu.assessments WHERE id = :assessmentId AND course_id = :courseId FOR UPDATE
                """, Map.of("assessmentId", assessmentId, "courseId", courseId));
        if (Boolean.TRUE.equals(assessment.get("ai_generation_started"))) {
            throw new IllegalStateException("Sources cannot be reopened after AI generation starts");
        }
        UUID ccctId = (UUID) assessment.get("ccct_profile_id");
        UUID previousId = (UUID) assessment.get("previous_published_ccct_id");
        if (Boolean.TRUE.equals(assessment.get("ccct_published_by_assessment")) && ccctId != null) {
            ensureSourceNotUsedElsewhere(assessmentId, "ccct_profile_id", ccctId);
            jdbc.update("""
                    UPDATE edu.ccct_profiles SET status = 'DRAFT', published_at = NULL,
                        version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id
                    """, Map.of("id", ccctId));
            if (previousId != null) {
                jdbc.update("""
                        UPDATE edu.ccct_profiles SET status = 'PUBLISHED', updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND status = 'ARCHIVED'
                        """, Map.of("id", previousId));
            }
        }
        UUID matrixId = (UUID) assessment.get("exam_matrix_id");
        if (Boolean.TRUE.equals(assessment.get("matrix_published_by_assessment")) && matrixId != null) {
            ensureSourceNotUsedElsewhere(assessmentId, "exam_matrix_id", matrixId);
            jdbc.update("""
                    UPDATE edu.exam_matrices SET lifecycle_status = 'DRAFT', published_by = NULL,
                        published_at = NULL, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id
                    """, Map.of("id", matrixId));
        }
        jdbc.update("""
                UPDATE edu.assessments
                SET current_step = 2, previous_published_ccct_id = NULL,
                    ccct_published_by_assessment = false, matrix_published_by_assessment = false,
                    ccct_profile_version = (SELECT version FROM edu.ccct_profiles WHERE id = ccct_profile_id),
                    exam_matrix_version = (SELECT version FROM edu.exam_matrices WHERE id = exam_matrix_id),
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                """, Map.of("assessmentId", assessmentId, "courseId", courseId));
    }

    public List<UUID> allMaterialIds(UUID courseId) {
        return jdbc.query("SELECT id FROM edu.materials WHERE course_id = :courseId ORDER BY created_at, id",
                Map.of("courseId", courseId), (rs, rowNum) -> rs.getObject("id", UUID.class));
    }

    public List<UUID> readyMaterialIds(UUID courseId) {
        return jdbc.query("""
                SELECT id FROM edu.materials
                WHERE course_id = :courseId AND status = 'UPLOADED' AND ingestion_status = 'READY'
                ORDER BY created_at, id
                """, Map.of("courseId", courseId), (rs, rowNum) -> rs.getObject("id", UUID.class));
    }

    public List<String> unreadyMaterialNames(UUID courseId) {
        return jdbc.query("""
                SELECT title FROM edu.materials
                WHERE course_id = :courseId AND (status <> 'UPLOADED' OR ingestion_status <> 'READY')
                ORDER BY title, id
                """, Map.of("courseId", courseId), (rs, rowNum) -> rs.getString("title"));
    }

    public String courseTitle(UUID courseId) {
        return jdbc.queryForObject("SELECT course_name FROM edu.courses WHERE id = :courseId",
                Map.of("courseId", courseId), String.class);
    }

    public void markGenerationStarted(UUID courseId, UUID assessmentId) {
        jdbc.update("""
                UPDATE edu.assessments SET ai_generation_started = true, lifecycle_status = 'GENERATING',
                    current_step = GREATEST(current_step, 6), version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                """, Map.of("assessmentId", assessmentId, "courseId", courseId));
    }

    private void updateSourceProgress(UUID courseId, UUID assessmentId, int step, UUID profileId,
            long profileVersion, boolean sourcePresent, boolean publishedByAssessment, UUID previousId) {
        jdbc.update("""
                UPDATE edu.assessments
                SET ccct_profile_id = :profileId, ccct_profile_version = :profileVersion,
                    previous_published_ccct_id = :previousId,
                    ccct_published_by_assessment = :publishedByAssessment,
                    current_step = GREATEST(current_step, :step), version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                """, new MapSqlParameterSource().addValue("profileId", profileId)
                .addValue("profileVersion", profileVersion).addValue("previousId", previousId)
                .addValue("publishedByAssessment", sourcePresent && publishedByAssessment)
                .addValue("step", step).addValue("assessmentId", assessmentId).addValue("courseId", courseId));
    }

    private void updateMatrixProgress(UUID courseId, UUID assessmentId, UUID matrixId,
            long matrixVersion, boolean publishedByAssessment) {
        jdbc.update("""
                UPDATE edu.assessments
                SET exam_matrix_id = :matrixId, exam_matrix_version = :matrixVersion,
                    matrix_published_by_assessment = :publishedByAssessment,
                    current_step = GREATEST(current_step, 5), version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :assessmentId AND course_id = :courseId
                """, Map.of("matrixId", matrixId, "matrixVersion", matrixVersion,
                "publishedByAssessment", publishedByAssessment, "assessmentId", assessmentId, "courseId", courseId));
    }

    private void ensureSourceNotUsedElsewhere(UUID assessmentId, String column, UUID sourceId) {
        String safeColumn = switch (column) {
            case "ccct_profile_id" -> "ccct_profile_id";
            case "exam_matrix_id" -> "exam_matrix_id";
            default -> throw new IllegalArgumentException("Unsupported source column");
        };
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM edu.assessments WHERE id <> :assessmentId AND "
                + safeColumn + " = :sourceId AND lifecycle_status <> 'ARCHIVED'",
                Map.of("assessmentId", assessmentId, "sourceId", sourceId), Integer.class);
        if (count != null && count > 0) throw new IllegalStateException("Source is already referenced by another assessment");
    }

    private String assessmentSelect() {
        return """
                SELECT a.id, a.course_id, a.title, a.description, a.unlimited,
                       a.time_limit_minutes, a.pass_score, a.start_time, a.due_time,
                       a.attempt_limited, a.variant_count, a.option_count, a.generation_mode,
                       a.allow_recently_used_questions, a.lifecycle_status, a.current_step,
                       a.ccct_profile_id, a.ccct_profile_version, a.exam_matrix_id,
                       a.exam_matrix_version, a.ai_generation_started, a.version,
                       a.created_by, a.published_by, a.published_at, a.created_at, a.updated_at
                FROM edu.assessments a
                """;
    }

    private AssessmentView mapAssessment(ResultSet rs, int rowNum) throws SQLException {
        return new AssessmentView(
                rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getString("title"), rs.getString("description"), rs.getBoolean("unlimited"),
                (Integer) rs.getObject("time_limit_minutes"), (Double) rs.getObject("pass_score"),
                rs.getObject("start_time", LocalDateTime.class), rs.getObject("due_time", LocalDateTime.class),
                (Integer) rs.getObject("attempt_limited"), (Integer) rs.getObject("variant_count"),
                (Integer) rs.getObject("option_count"), GenerationMode.valueOf(rs.getString("generation_mode")),
                rs.getBoolean("allow_recently_used_questions"), LifecycleStatus.valueOf(rs.getString("lifecycle_status")),
                rs.getInt("current_step"), rs.getObject("ccct_profile_id", UUID.class),
                (Long) rs.getObject("ccct_profile_version"), rs.getObject("exam_matrix_id", UUID.class),
                (Long) rs.getObject("exam_matrix_version"), rs.getBoolean("ai_generation_started"),
                rs.getLong("version"), rs.getObject("created_by", UUID.class),
                rs.getObject("published_by", UUID.class), rs.getObject("published_at", LocalDateTime.class),
                rs.getObject("created_at", LocalDateTime.class), rs.getObject("updated_at", LocalDateTime.class), null);
    }

    public record MatrixCellRef(UUID matrixRowId, UUID customLearningLevelId) {
    }
}
