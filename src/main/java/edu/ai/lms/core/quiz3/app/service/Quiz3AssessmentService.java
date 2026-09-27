package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import static edu.ai.lms.core.quiz3.app.domain.Quiz3Models.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.CourseTeacherAccess;
import edu.ai.lms.core.course.app.service.matrix.MatrixService;
import edu.ai.lms.core.quiz3.api.CodeRunnerContractSnapshotApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Quiz3AssessmentService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final CourseTeacherAccess access;
    private final LoadSignedInUserPort signedInUserPort;
    private final MatrixService matrixService;
    private final Quiz3GenerationDispatcher dispatcher;
    private final Quiz3QBank2RegenerationService qbank2Regeneration;
    private final Quiz3ReleaseCoordinator releases;
    private final CodeRunnerContractSnapshotApi codeRunnerContracts;

    public List<AssessmentView> list(UUID courseId) {
        requireTeacher(courseId);
        return jdbc.query(baseSelect() + " WHERE a.course_id=? ORDER BY a.updated_at DESC, a.id",
                (rs, row) -> mapAssessment(rs), courseId);
    }

    public AssessmentView get(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        return required(courseId, assessmentId);
    }

    @Transactional
    public AssessmentView create(UUID courseId, Configuration input) {
        CurrentActor actor = requireTeacher(courseId);
        validateConfiguration(input);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.quiz3_assessments
                    (id, course_id, title, description, unlimited, time_limit_minutes, pass_score,
                     start_time, due_time, attempt_limited, variant_count, created_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, courseId, input.title().trim(), trim(input.description()),
                Boolean.TRUE.equals(input.unlimited()), input.timeLimitMinutes(), input.passScore(),
                input.startTime(), input.dueTime(), input.attemptLimited(),
                input.variantCount() == null ? 1 : input.variantCount(), actor.id());
        return required(courseId, id);
    }

    @Transactional
    public AssessmentView update(UUID courseId, UUID assessmentId, long expectedVersion, Configuration input) {
        requireTeacher(courseId);
        AssessmentView current = required(courseId, assessmentId);
        if ("PUBLISHED".equals(current.lifecycleStatus())) throw conflict("Published Quiz3 assessment is read-only");
        validateConfiguration(input);
        int updated = jdbc.update("""
                UPDATE edu.quiz3_assessments SET title=?, description=?, unlimited=?, time_limit_minutes=?,
                    pass_score=?, start_time=?, due_time=?, attempt_limited=?, variant_count=?,
                    version=version+1, updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=? AND version=?
                """, input.title().trim(), trim(input.description()), Boolean.TRUE.equals(input.unlimited()),
                input.timeLimitMinutes(), input.passScore(), input.startTime(), input.dueTime(), input.attemptLimited(),
                input.variantCount() == null ? current.variantCount() : input.variantCount(),
                assessmentId, courseId, expectedVersion);
        if (updated != 1) throw conflict("Quiz3 assessment changed; reload and try again");
        return required(courseId, assessmentId);
    }

    public ContextView context(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        List<SourceOption> matrices = matrixService.list(courseId).matrices().stream()
                .map(matrix -> new SourceOption(matrix.id(), matrix.title(), matrix.lifecycleStatus().name(),
                        matrix.version(), MatrixMode.valueOf(matrix.mode().name())))
                .toList();
        // The course's single CCCT is derived from the selected Matrix.
        return new ContextView(assessment, matrices);
    }

    @Transactional
    public ContextView selectContext(UUID courseId, UUID assessmentId, ContextSelection input) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.baseGenerationJobId() != null) throw conflict("Sources cannot change after Quiz3 generation starts");
        if (input == null || input.matrix() == null) {
            throw badRequest("A Matrix selection is required");
        }
        MatrixSelection matrix = resolveMatrix(courseId, input.matrix());
        MatrixService.MatrixDetail selected = matrixService.get(courseId, matrix.id());
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET ccct_profile_id=?, ccct_published_at=?,
                    matrix_id=?, matrix_version=?, current_step=GREATEST(current_step,2), version=version+1,
                    updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=?
                """, selected.ccctProfileId(), selected.ccctPublishedAt(), matrix.id(), matrix.version(), assessmentId, courseId);
        return context(courseId, assessmentId);
    }

    @Transactional
    public AssessmentView confirmMatrix(UUID courseId, UUID assessmentId, long expectedVersion) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.currentStep() < 2) throw badRequest("Select a Matrix before confirming it");
        if (assessment.matrixId() == null) throw badRequest("Select a Matrix first");
        MatrixService.MatrixDetail matrix = matrixService.get(courseId, assessment.matrixId());
        if (matrix.version() != expectedVersion) throw conflict("Matrix changed; reload and review it again");
        if (matrix.lifecycleStatus() == MatrixService.LifecycleStatus.DRAFT) matrix = matrixService.publish(courseId, assessment.matrixId(), new MatrixService.PublishRequest(expectedVersion));
        if (matrix.ccctPublishedAt() == null) throw conflict("The published Matrix has no CCCT revision snapshot");
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET ccct_profile_id=?,ccct_published_at=?,
                    matrix_version=?, current_step=GREATEST(current_step,5),version=version+1,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND course_id=?
                """, matrix.ccctProfileId(), matrix.ccctPublishedAt(), matrix.version(), assessmentId, courseId);
        return required(courseId, assessmentId);
    }

    @Transactional
    public GenerationView startGeneration(UUID courseId, UUID assessmentId) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.currentStep() < 5 || assessment.matrixId() == null) {
            throw badRequest("Confirm CCCT and Matrix before creating Quiz 001");
        }
        if (assessment.baseGenerationJobId() != null) {
            GenerationView current = generation(courseId, assessmentId);
            if (!"FAILED".equals(current.status()) && !"TIMED_OUT".equals(current.status())) return current;
        }
        List<String> unready = jdbc.query("""
                SELECT title FROM edu.materials WHERE course_id=?
                  AND (status<>'UPLOADED' OR ingestion_status<>'READY') ORDER BY title
                """, (rs, row) -> rs.getString(1), courseId);
        if (!unready.isEmpty()) throw conflict("Materials are not READY: " + String.join(", ", unready));
        List<UUID> materials = jdbc.query("SELECT id FROM edu.materials WHERE course_id=? ORDER BY created_at,id",
                (rs, row) -> rs.getObject(1, UUID.class), courseId);
        if (materials.isEmpty()) throw conflict("The course has no source materials");
        UUID jobId = UUID.randomUUID();
        ObjectNode snapshot = buildSnapshot(assessment, jobId, materials);
        int total = generatedQuestionTotal(snapshot);
        String json = toJson(snapshot);
        jdbc.update("""
                INSERT INTO edu.quiz3_generation_jobs
                    (id, assessment_id, course_id, matrix_id,matrix_version,requested_by,
                     request_snapshot, payload_hash, idempotency_key, total_question_count, next_attempt_at)
                VALUES (?,?,?,?,?, ?,CAST(? AS jsonb),?,?,?,CURRENT_TIMESTAMP)
                """, jobId, assessmentId, courseId, assessment.matrixId(), assessment.matrixVersion(),
                actor.id(), json, sha256(json), snapshot.path("idempotencyKey").asText(), total);
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET base_generation_job_id=?, lifecycle_status='GENERATING',
                    version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=? AND course_id=?
                """, jobId, assessmentId, courseId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { dispatcher.dispatch(jobId); }
        });
        return generation(courseId, assessmentId);
    }

    public GenerationView generation(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.baseGenerationJobId() == null) {
            return new GenerationView(null, "NOT_STARTED", null, 0, null, null, null, 0, 0);
        }
        return jdbc.query("""
                SELECT id,status,phase,attempt_count,quiz_id,error_code,error_message,
                       generated_question_count,total_question_count
                FROM edu.quiz3_generation_jobs WHERE id=? AND assessment_id=?
                """, rs -> rs.next() ? new GenerationView(rs.getObject(1, UUID.class), rs.getString(2),
                rs.getString(3), rs.getInt(4), rs.getObject(5, UUID.class), rs.getString(6), rs.getString(7),
                rs.getInt(8), rs.getInt(9)) : null, assessment.baseGenerationJobId(), assessmentId);
    }

    @Transactional
    public void progress(ProgressPayload payload) {
        if (payload == null || payload.jobId() == null) throw badRequest("jobId is required");
        int updated = jdbc.update("""
                UPDATE edu.quiz3_generation_jobs SET generated_question_count=LEAST(total_question_count,?),
                    updated_at=CURRENT_TIMESTAMP WHERE id=? AND assessment_id=? AND status='PROCESSING'
                    AND attempt_count=?
                """, Math.max(0, payload.generatedQuestionCount()), payload.jobId(), payload.assessmentId(), payload.attempt());
        if (updated == 0) throw conflict("Quiz3 progress callback does not match an active attempt");
    }

    @Transactional
    public void webhook(WebhookPayload payload) {
        if (payload == null || payload.jobId() == null || payload.eventId() == null) throw badRequest("eventId and jobId are required");
        Map<String, Object> job = jdbc.queryForMap("""
                SELECT assessment_id,course_id,request_snapshot::text,status,total_question_count
                FROM edu.quiz3_generation_jobs WHERE id=? FOR UPDATE
                """, payload.jobId());
        if (Set.of("COMPLETED", "FAILED", "TIMED_OUT").contains(String.valueOf(job.get("status")))) return;
        UUID assessmentId = (UUID) job.get("assessment_id");
        if (!assessmentId.equals(payload.assessmentId())) throw badRequest("assessmentId does not match Quiz3 job");
        if ("FAILED".equalsIgnoreCase(payload.status())) {
            jdbc.update("""
                    UPDATE edu.quiz3_generation_jobs SET status='FAILED',error_code=?,error_message=?,callback_event_id=?,
                        lease_until=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, payload.errorCode(), payload.errorMessage(), payload.eventId(), payload.jobId());
            jdbc.update("UPDATE edu.quiz3_assessments SET lifecycle_status='DRAFT',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?", assessmentId);
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(payload.status()) || payload.quiz() == null) throw badRequest("Invalid Quiz3 webhook status");
        JsonNode snapshot = readJson(String.valueOf(job.get("request_snapshot")));
        List<GeneratedQuestion> questions = payload.quiz().questions() == null ? List.of() : payload.quiz().questions();
        try {
            validateCallback(snapshot, questions, ((Number) job.get("total_question_count")).intValue());
            questions.stream().filter(question -> "CODE_RUNNER".equals(question.type()))
                    .forEach(question -> codeRunnerContracts.normalizeForReview(question.codeRunnerContract().toString()));
        } catch (RuntimeException invalidOutput) {
            String message = invalidOutput.getMessage() == null
                    ? "AI returned an invalid Quiz3 payload" : invalidOutput.getMessage();
            jdbc.update("""
                    UPDATE edu.quiz3_generation_jobs SET status='FAILED',phase='DONE',error_code='INVALID_AI_OUTPUT',
                        error_message=?,callback_event_id=?,lease_until=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                    WHERE id=?
                    """, message.substring(0, Math.min(1000, message.length())), payload.eventId(), payload.jobId());
            jdbc.update("UPDATE edu.quiz3_assessments SET lifecycle_status='DRAFT',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?", assessmentId);
            return;
        }
        AssessmentView assessment = required((UUID) job.get("course_id"), assessmentId);
        UUID quizId = UUID.randomUUID();
        UUID teacherId = jdbc.queryForObject("SELECT created_by FROM edu.quiz3_assessments WHERE id=?", UUID.class, assessmentId);
        jdbc.update("""
                INSERT INTO edu.quizzes
                    (id,quiz_title,description,unlimited,time_limit_minutes,pass_score,start_time,due_time,
                     attempt_limited,course_id,teacher_id,display,lifecycle_status,source_type)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,false,'REVIEW_PENDING','AI_GENERATED')
                """, quizId, assessment.title()+" - 001", assessment.description(), assessment.unlimited(),
                assessment.timeLimitMinutes(), assessment.passScore(), assessment.startTime(), assessment.dueTime(),
                assessment.attemptLimited(), assessment.courseId(), teacherId);
        for (int index=0; index<questions.size(); index++) persistQuestion(payload.jobId(), quizId, questions.get(index), index, null,
                snapshotContent(snapshot, questions.get(index).matrixRowId()));
        UUID variantId = UUID.randomUUID();
        long seed = assessmentId.getMostSignificantBits() ^ assessmentId.getLeastSignificantBits();
        jdbc.update("""
                INSERT INTO edu.quiz3_assessment_variants
                    (id,assessment_id,quiz_id,variant_code,order_index,shuffle_seed,variant_kind,coverage_status,coverage_snapshot)
                VALUES (?,?,?,'001',0,?,'BASE','COMPLIANT',CAST(? AS jsonb))
                """, variantId, assessmentId, quizId, seed, toJson(Map.of("status","COMPLIANT","questionCount",questions.size())));
        jdbc.update("""
                UPDATE edu.quiz3_generation_jobs SET status='COMPLETED',phase='DONE',quiz_id=?,callback_event_id=?,
                    generated_question_count=total_question_count,completed_at=CURRENT_TIMESTAMP,lease_until=NULL,updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """, quizId, payload.eventId(), payload.jobId());
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET lifecycle_status='REVIEW_PENDING',current_step=GREATEST(current_step,6),
                    version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, assessmentId);
    }

    public List<VariantView> variants(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId); required(courseId, assessmentId);
        return jdbc.query("""
                SELECT id,quiz_id,variant_code,variant_kind,order_index,shuffle_seed,coverage_status
                FROM edu.quiz3_assessment_variants WHERE assessment_id=? ORDER BY order_index
                """, (rs,row) -> new VariantView(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),
                rs.getString(4),rs.getInt(5),rs.getLong(6),rs.getString(7),questions(rs.getObject(2,UUID.class))), assessmentId);
    }

    @Transactional
    public List<VariantView> confirmBase(UUID courseId, UUID assessmentId) {
        CurrentActor actor=requireTeacher(courseId); AssessmentView assessment=required(courseId,assessmentId);
        UUID quizId=baseQuiz(assessmentId); validateQuiz(quizId);
        jdbc.update("UPDATE edu.quizzes SET lifecycle_status='READY_TO_PUBLISH',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",quizId);
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET base_confirmed_by=?,base_confirmed_at=CURRENT_TIMESTAMP,
                    current_step=GREATEST(current_step,7),version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """,actor.id(),assessmentId);
        return variants(courseId,assessmentId);
    }

    @Transactional
    public List<VariantView> createVariants(UUID courseId, UUID assessmentId, int variantCount) {
        requireTeacher(courseId); AssessmentView assessment=required(courseId,assessmentId);
        if (variantCount<1||variantCount>20) throw badRequest("variantCount must be between 1 and 20");
        UUID baseQuiz=baseQuiz(assessmentId); validateQuiz(baseQuiz);
        int existing=jdbc.queryForObject("SELECT COUNT(*) FROM edu.quiz3_assessment_variants WHERE assessment_id=?",Integer.class,assessmentId);
        for(int order=existing;order<variantCount;order++) cloneVariant(assessment,baseQuiz,order);
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET variant_count=?,current_step=GREATEST(current_step,8),
                    lifecycle_status='READY_TO_PUBLISH',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """,variantCount,assessmentId);
        return variants(courseId,assessmentId);
    }

    @Transactional
    public AssessmentView confirmVariantPreview(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        jdbc.update("UPDATE edu.quiz3_assessments SET current_step=GREATEST(current_step, 9), version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=?", assessmentId);
        return required(courseId, assessmentId);
    }

    @Transactional
    public List<VariantView> reopenBase(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId); required(courseId,assessmentId);
        jdbc.update("DELETE FROM edu.quiz3_assessment_variants WHERE assessment_id=? AND variant_kind='SHUFFLED'",assessmentId);
        jdbc.update("UPDATE edu.quizzes SET lifecycle_status='REVIEW_PENDING',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",baseQuiz(assessmentId));
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET current_step=6,lifecycle_status='REVIEW_PENDING',base_confirmed_by=NULL,
                    base_confirmed_at=NULL,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """,assessmentId);
        return variants(courseId,assessmentId);
    }

    @Transactional
    public List<VariantView> updateQuestion(UUID courseId,UUID assessmentId,UUID variantId,UUID questionId,QuestionUpdate input){
        requireTeacher(courseId); required(courseId,assessmentId);
        if(input==null||input.content()==null||input.content().isBlank()) throw badRequest("Question content cannot be blank");
        String questionType=jdbc.queryForObject("""
                SELECT q.type FROM edu.questions q JOIN edu.quiz3_assessment_variants v ON v.quiz_id=q.quiz_id
                WHERE q.id=? AND v.id=? AND v.assessment_id=?
                """,String.class,questionId,variantId,assessmentId);
        if("MULTIPLE_CHOICE".equals(questionType)) validateAnswers(input.answers());
        else if(input.answers()!=null&&!input.answers().isEmpty()) throw badRequest("Code Runner questions cannot contain answer rows");
        int updated=jdbc.update("""
                UPDATE edu.questions SET content=?,content_format=?,explanation=?,explanation_format=?,version=version+1,
                    updated_at=CURRENT_TIMESTAMP WHERE id=? AND quiz_id=(SELECT quiz_id FROM edu.quiz3_assessment_variants
                    WHERE id=? AND assessment_id=?) AND version=?
                """,input.content(),format(input.contentFormat()),trim(input.explanation()),format(input.explanationFormat()),
                questionId,variantId,assessmentId,input.expectedQuestionVersion());
        if(updated!=1) throw conflict("Question changed; reload and try again");
        jdbc.update("DELETE FROM edu.answers WHERE question_id=?",questionId);
        for(AnswerInput answer:input.answers()==null?List.<AnswerInput>of():input.answers()) jdbc.update("""
                INSERT INTO edu.answers(content,is_correct,content_format,question_id) VALUES (?,?,?,?)
                """,answer.content(),answer.correct(),format(answer.contentFormat()),questionId);
        if("MULTIPLE_CHOICE".equals(questionType)) qbank2Regeneration.applyOnQuestionSave(assessmentId, variantId, questionId, input.expectedQuestionVersion());
        recalculateCoverageInternal(assessmentId,variantId);
        return variants(courseId,assessmentId);
    }

    @Transactional
    public List<VariantView> recalculateCoverage(UUID courseId,UUID assessmentId,UUID variantId){
        requireTeacher(courseId); required(courseId,assessmentId); recalculateCoverageInternal(assessmentId,variantId);
        return variants(courseId,assessmentId);
    }

    @Transactional
    public PublishResult publish(UUID courseId,UUID assessmentId){
        CurrentActor actor=requireTeacher(courseId); AssessmentView assessment=required(courseId,assessmentId);
        if(!"READY_TO_PUBLISH".equals(assessment.lifecycleStatus())) {
            throw conflict("Only a ready Quiz3 assessment can be published");
        }
        List<VariantView> variants=variants(courseId,assessmentId);
        if(variants.size()!=assessment.variantCount()) throw conflict("Create every configured Quiz3 variant before publish");
        for(VariantView variant:variants){
            recalculateCoverageInternal(assessmentId,variant.id()); validateQuiz(variant.quizId());
            String coverage=jdbc.queryForObject("SELECT coverage_status FROM edu.quiz3_assessment_variants WHERE id=?",String.class,variant.id());
            if(!"COMPLIANT".equals(coverage)) throw conflict("Every Quiz3 variant must be coverage compliant");
        }
        LocalDateTime publishedAt=LocalDateTime.now();
        jdbc.update("""
                UPDATE edu.quizzes SET lifecycle_status='PUBLISHED',display=true,published_by=?,published_at=?,
                    version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN
                    (SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE assessment_id=?)
                """,actor.id(),publishedAt,assessmentId);
        releases.publish(variants.stream().map(VariantView::quizId).toList(),actor.id(),publishedAt);
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET lifecycle_status='PUBLISHED',current_step=9,published_by=?,published_at=?,
                    version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """,actor.id(),publishedAt,assessmentId);
        return new PublishResult(assessmentId,"PUBLISHED",variants.size(),publishedAt);
    }

    @Transactional
    public AssessmentView unpublish(UUID courseId,UUID assessmentId){
        CurrentActor actor=requireTeacher(courseId); AssessmentView assessment=required(courseId,assessmentId);
        if(!"PUBLISHED".equals(assessment.lifecycleStatus())) {
            throw conflict("Only a published Quiz3 assessment can be unpublished");
        }
        List<UUID> quizIds=jdbc.queryForList(
                "SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE assessment_id=?",
                UUID.class,assessmentId);
        jdbc.update("""
                UPDATE edu.quizzes SET lifecycle_status='READY_TO_PUBLISH',display=false,published_by=NULL,published_at=NULL,
                    version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN
                    (SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE assessment_id=?)
                """,assessmentId);
        releases.deactivate(quizIds);
        jdbc.update("""
                UPDATE edu.quiz3_assessments SET lifecycle_status='READY_TO_PUBLISH',published_by=NULL,published_at=NULL,
                    last_unpublished_by=?,last_unpublished_at=CURRENT_TIMESTAMP,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?
                """,actor.id(),assessmentId);
        return required(courseId,assessmentId);
    }

    private ObjectNode buildSnapshot(AssessmentView assessment,UUID jobId,List<UUID> materialIds){
        ObjectNode root=objectMapper.createObjectNode(); root.put("jobId",jobId.toString()); root.put("assessmentId",assessment.id().toString()); root.put("contractVersion",5);
        ObjectNode course=root.putObject("course"); course.put("id",assessment.courseId().toString());
        course.put("name",jdbc.queryForObject("SELECT course_name FROM edu.courses WHERE id=?",String.class,assessment.courseId()));
        UUID matrixId=assessment.matrixId();
        long matrixVersion=assessment.matrixVersion();
        MatrixService.MatrixDetail selectedMatrix=matrixService.get(assessment.courseId(),matrixId);
        if(selectedMatrix.version()!=matrixVersion)throw conflict("Matrix changed; reload and review it again");
        if(!Objects.equals(selectedMatrix.ccctProfileId(),assessment.ccctProfileId())
                || !Objects.equals(selectedMatrix.ccctPublishedAt(),assessment.ccctPublishedAt())) {
            throw conflict("Matrix and CCCT revision snapshot no longer match the confirmed Quiz3 source");
        }
        root.put("quizName",assessment.title()+" - 001");
        ObjectNode matrix=createGenerationMatrix(objectMapper,matrixId,selectedMatrix.title());
        root.set("matrix",matrix);
        ArrayNode rows=matrix.putArray("rows");
        List<Map<String,Object>> cells;
        List<Integer> allocated;
        cells=jdbc.queryForList("""
            SELECT r.id row_id,r.chapter_id,coalesce(rc.title,cc.title,c.title) chapter_name,r.mapped_learning_outcome_id lo_id,
                   lo.prefix_number,lo.description,l.id level_id,l.name level_name,ec.question_count,
                   coalesce(ri.id,i.id) content_item_id,coalesce(ri.content_code,i.content_code) content_code,
                   coalesce(ri.content,i.content) content_detail
            FROM edu.exam_matrix_rows r JOIN edu.exam_matrices m ON m.id=r.matrix_id
            LEFT JOIN edu.chapters c ON c.chapter_id=r.chapter_id
            LEFT JOIN edu.ccct_items i ON m.ccct_published_at IS NULL AND i.id=r.ccct_item_id
            LEFT JOIN edu.ccct_chapters cc ON cc.id=i.chapter_id
            LEFT JOIN edu.ccct_revision_items ri ON m.ccct_published_at IS NOT NULL AND ri.profile_id=m.ccct_profile_id AND ri.published_at=m.ccct_published_at AND ri.id=r.ccct_item_id
            LEFT JOIN edu.ccct_revision_chapters rc ON rc.profile_id=ri.profile_id AND rc.published_at=ri.published_at AND rc.id=ri.chapter_id
            JOIN edu.learning_outcomes lo ON lo.id=r.mapped_learning_outcome_id
            JOIN edu.exam_matrix_cells ec ON ec.row_id=r.id
            JOIN edu.exam_matrix_custom_learning_levels l ON l.id=ec.custom_learning_level_id
            WHERE r.matrix_id=? ORDER BY r.sort_order,r.id,l.sort_order,l.id
            """,matrixId);
        String mode=selectedMatrix.mode().name();
        allocated="AUTOMATIC".equals(mode)
                ? MatrixService.allocate(cells.size(),jdbc.queryForObject("SELECT total_question_count FROM edu.exam_matrices WHERE id=?",Integer.class,matrixId))
                : cells.stream().map(cell->((Number)cell.get("question_count")).intValue()).toList();
        Map<UUID,List<MatrixService.AssetCoverage>> coverageByLevel=new HashMap<>();
        selectedMatrix.customLearningLevels().forEach(level -> coverageByLevel.put(level.id(),level.assetCoverages()));
        Map<UUID,ObjectNode> rowNodes=new LinkedHashMap<>();
        for(int index=0;index<cells.size();index++){
            int count=allocated.get(index); if(count<=0) continue;
            Map<String,Object> cell=cells.get(index); UUID rowId=(UUID)cell.get("row_id");
            ObjectNode row=rowNodes.computeIfAbsent(rowId,ignored->{
                ObjectNode node=rows.addObject(); node.put("id",rowId.toString());
                ObjectNode chapter=node.putObject("chapter"); chapter.put("id",String.valueOf(cell.get("chapter_id"))); chapter.put("name",String.valueOf(cell.get("chapter_name")));
                ObjectNode lo=node.putObject("learningOutcome"); lo.put("id",String.valueOf(cell.get("lo_id")));
                Object prefix=cell.get("prefix_number"); String description=String.valueOf(cell.get("description"));
                lo.put("name",prefix==null?description:"LO"+prefix+" - "+description);
                ObjectNode content=node.putObject("content"); content.put("id",String.valueOf(cell.get("content_item_id"))); content.put("detail",String.valueOf(cell.get("content_detail"))); content.put("contentCode",String.valueOf(cell.get("content_code"))); node.putArray("cells"); return node;
            });
            ObjectNode node=row.withArray("cells").addObject(); node.put("customLearningLevelId",String.valueOf(cell.get("level_id")));
            node.put("customLearningLevelName",String.valueOf(cell.get("level_name"))); node.put("questionCount",count);
            ArrayNode allocations=node.putArray("assetAllocations");
            for (AssetAllocation allocation : allocateAssetCoverage(count,coverageByLevel.get((UUID)cell.get("level_id")))) {
                ObjectNode value=allocations.addObject(); value.put("assetType",allocation.assetType());
                value.put("questionType",allocation.questionType()); value.put("questionCount",allocation.questionCount());
                value.put("effectiveAssetType",allocation.effectiveAssetType());
                if (allocation.fallbackReason()!=null) value.put("fallbackReason",allocation.fallbackReason());
            }
        }
        ArrayNode materials=root.putArray("materialIds"); materialIds.forEach(id->materials.add(id.toString()));
        root.put("idempotencyKey","quiz3-base-"+assessment.id()+"-matrix-"+matrixVersion+"-job-"+jobId); return root;
    }

    static ObjectNode createGenerationMatrix(ObjectMapper objectMapper,UUID matrixId,String matrixName){
        ObjectNode matrix=objectMapper.createObjectNode(); matrix.put("id",matrixId.toString()); matrix.put("name",matrixName);
        return matrix;
    }

    private static String slug(String value){ return value.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }

    static List<Integer> allocateRelationCounts(int selectedCellCount,int totalQuestionCount){
        if(selectedCellCount<=0)throw badRequest("Automatic mode needs at least one selected relation");
        if(totalQuestionCount<=0)throw badRequest("Automatic mode total question count must be greater than zero");
        int base=totalQuestionCount/selectedCellCount,remainder=totalQuestionCount%selectedCellCount; List<Integer> counts=new ArrayList<>();
        for(int index=0;index<selectedCellCount;index++)counts.add(base+(index<remainder?1:0)); return counts;
    }

    static List<AssetAllocation> allocateAssetCoverage(int total,List<MatrixService.AssetCoverage> coverages){
        List<MatrixService.AssetType> order=List.of(MatrixService.AssetType.TEXT,MatrixService.AssetType.IMAGE,MatrixService.AssetType.CODE_MCQ,MatrixService.AssetType.CODE_RUNNER);
        Map<MatrixService.AssetType,Integer> percentages=new HashMap<>();
        for (MatrixService.AssetCoverage coverage:coverages==null?List.<MatrixService.AssetCoverage>of():coverages) percentages.put(coverage.assetType(),coverage.percentage());
        List<Integer> counts=new ArrayList<>(); List<Integer> remainders=new ArrayList<>(); int assigned=0;
        for (MatrixService.AssetType type:order){int scaled=total*percentages.getOrDefault(type,type==MatrixService.AssetType.TEXT?100:0); counts.add(scaled/100); remainders.add(scaled%100); assigned+=scaled/100;}
        while(assigned<total){int winner=0;for(int index=1;index<remainders.size();index++)if(remainders.get(index)>remainders.get(winner))winner=index;counts.set(winner,counts.get(winner)+1);remainders.set(winner,-1);assigned++;}
        List<AssetAllocation> result=new ArrayList<>();
        for(int index=0;index<order.size();index++){String asset=order.get(index).name();String type="CODE_RUNNER".equals(asset)?"CODE_RUNNER":"MULTIPLE_CHOICE";String effective="IMAGE".equals(asset)?"TEXT":asset;result.add(new AssetAllocation(asset,type,counts.get(index),effective,"IMAGE".equals(asset)?"IMAGE_MEDIA_NOT_SUPPORTED":null));}
        return result;
    }
    record AssetAllocation(String assetType,String questionType,int questionCount,String effectiveAssetType,String fallbackReason) { }

    /** Counts only the allocation slots that are sent to the Quiz3 AI pipeline. */
    static int generatedQuestionTotal(JsonNode snapshot) {
        int total = 0;
        for (JsonNode row : snapshot.path("matrix").path("rows")) {
            for (JsonNode cell : row.path("cells")) {
                int cellQuota = cell.path("questionCount").asInt(-1);
                if (cellQuota <= 0) throw badRequest("Quiz3 snapshot cell question count must be greater than zero");
                int allocated = 0;
                for (JsonNode allocation : cell.path("assetAllocations")) {
                    int count = allocation.path("questionCount").asInt(-1);
                    if (count < 0) throw badRequest("Quiz3 snapshot asset allocation question count cannot be negative");
                    allocated += count;
                }
                if (allocated != cellQuota) {
                    throw badRequest("Quiz3 snapshot asset allocation total does not match cell question count");
                }
                total += allocated;
            }
        }
        return total;
    }

    static void validateCallback(JsonNode snapshot,List<GeneratedQuestion> questions,int total){
        if(questions.size()!=total) throw badRequest("Quiz3 callback total question count does not match snapshot");
        Map<String,Integer> expected=new HashMap<>(),actual=new HashMap<>();
        for(JsonNode row:snapshot.path("matrix").path("rows")) for(JsonNode cell:row.path("cells")) for(JsonNode allocation:cell.path("assetAllocations")) {
            int count=allocation.path("questionCount").asInt();
            if(count>0) expected.put(allocationKey(row.path("id").asText(),cell.path("customLearningLevelId").asText(),row.path("learningOutcome").path("id").asText(),allocation.path("assetType").asText(),allocation.path("questionType").asText()),count);
        }
        for(GeneratedQuestion question:questions){
            if(question.matrixRowId()==null||question.customLearningLevelId()==null||question.learningOutcomeId()==null) throw badRequest("Quiz3 callback provenance is incomplete");
            if("MULTIPLE_CHOICE".equals(question.type())) validateGeneratedAnswers(question.answers());
            else if("CODE_RUNNER".equals(question.type())) {
                if(question.answers()!=null&&!question.answers().isEmpty()) throw badRequest("Quiz3 Code Runner callback cannot contain answers");
                if(question.codeRunnerContract()==null||question.codeRunnerContract().isNull()) throw badRequest("Quiz3 Code Runner callback needs codeRunnerContract");
            } else throw badRequest("Quiz3 accepts only MULTIPLE_CHOICE or CODE_RUNNER");
            if(question.assetType()==null||question.effectiveAssetType()==null) throw badRequest("Quiz3 callback asset provenance is incomplete");
            if("IMAGE".equals(question.assetType()) && !"TEXT".equals(question.effectiveAssetType())) throw badRequest("Quiz3 IMAGE questions must use the text fallback");
            actual.merge(allocationKey(question.matrixRowId().toString(),question.customLearningLevelId().toString(),question.learningOutcomeId().toString(),question.assetType(),question.type()),1,Integer::sum);
        }
        if(!expected.equals(actual)) throw badRequest("Quiz3 callback distribution does not match the matrix snapshot");
    }

    private JsonNode snapshotContent(JsonNode snapshot, UUID matrixRowId) {
        for (JsonNode row : snapshot.path("matrix").path("rows")) {
            if (matrixRowId.toString().equals(row.path("id").asText())) return row.path("content");
        }
        return null;
    }

    private void persistQuestion(UUID jobId,UUID quizId,GeneratedQuestion source,int order,UUID sourceQuestionId,JsonNode content){
        UUID id=UUID.randomUUID(); jdbc.update("""
                INSERT INTO edu.questions(id,content,content_format,explanation,explanation_format,type,quiz_id,review_status,source_type,source_question_id,order_index)
                VALUES (?,?,'PLAIN_TEXT',NULL,'PLAIN_TEXT',?,?,'APPROVED',?,?,?)
                """,id,source.content(),source.type(),quizId,sourceQuestionId==null?"AI_GENERATED":"QUIZ_CLONE",sourceQuestionId,order);
        for(GeneratedAnswer answer:source.answers()==null?List.<GeneratedAnswer>of():source.answers()) jdbc.update("INSERT INTO edu.answers(content,is_correct,explanation,question_id) VALUES (?,?,?,?)",answer.content(),answer.isCorrect(),answer.explanation(),id);
        if("CODE_RUNNER".equals(source.type())) {
            if(sourceQuestionId==null) codeRunnerContracts.createQuestionRevision(id,source.codeRunnerContract().toString());
            else codeRunnerContracts.cloneLatestQuestionRevision(sourceQuestionId,id);
        }
        jdbc.update("INSERT INTO edu.question_learning_outcomes(question_id,learning_outcome_id) VALUES (?,?) ON CONFLICT DO NOTHING",id,source.learningOutcomeId());
        JsonNode reference=source.qbank2Reference()==null?null:objectMapper.valueToTree(source.qbank2Reference());
        String selectionMode=reference==null?null:reference.path("selectionMode").asText(null);
        List<String> groupIds=reference==null?List.of():reference.path("contentGroup").hasNonNull("id")?List.of(reference.path("contentGroup").path("id").asText()):List.of();
        List<String> groupCodes=reference==null?List.of():reference.path("contentGroup").hasNonNull("code")?List.of(reference.path("contentGroup").path("code").asText()):List.of();
        List<String> exampleIds=reference==null?List.of():reference.path("example").hasNonNull("id")?List.of(reference.path("example").path("id").asText()):List.of();
        Map<String,Object> allocationSnapshot=new LinkedHashMap<>();
        allocationSnapshot.put("matrixRowId",source.matrixRowId()); allocationSnapshot.put("customLearningLevelId",source.customLearningLevelId());
        allocationSnapshot.put("learningOutcomeId",source.learningOutcomeId()); allocationSnapshot.put("assetType",source.assetType());
        allocationSnapshot.put("effectiveAssetType",source.effectiveAssetType()); if(content!=null&&!content.isMissingNode()&&!content.isNull()) allocationSnapshot.put("content",content);
        jdbc.update("""
                INSERT INTO edu.quiz3_question_provenance(question_id,generation_job_id,matrix_row_id,matrix_custom_learning_level_id,
                    learning_outcome_id,source_question_id,allocation_snapshot,requested_asset_type,effective_asset_type,asset_fallback_reason,
                    qbank2_selection_mode,qbank2_content_group_ids,qbank2_content_group_codes,qbank2_example_ids,qbank2_reference_snapshot)
                VALUES (?,?,?,?,?,?,CAST(? AS jsonb),?,?,?, ?,CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb))
                """,id,jobId,source.matrixRowId(),source.customLearningLevelId(),source.learningOutcomeId(),sourceQuestionId,
                toJson(allocationSnapshot),source.assetType(),source.effectiveAssetType(),source.assetFallbackReason(),
                selectionMode,toJson(groupIds),toJson(groupCodes),toJson(exampleIds),reference==null?null:toJson(source.qbank2Reference()));
    }

    private void cloneVariant(AssessmentView assessment,UUID baseQuiz,int order){
        UUID quizId=UUID.randomUUID(),variantId=UUID.randomUUID();long seed=assessment.id().getMostSignificantBits()^assessment.id().getLeastSignificantBits()^order;
        String code="%03d".formatted(order+1); jdbc.update("""
                INSERT INTO edu.quizzes(id,quiz_title,description,unlimited,time_limit_minutes,pass_score,start_time,due_time,attempt_limited,
                    course_id,teacher_id,display,lifecycle_status,source_type,original_quiz_id)
                SELECT ?,?,description,unlimited,time_limit_minutes,pass_score,start_time,due_time,attempt_limited,course_id,teacher_id,false,
                    'READY_TO_PUBLISH','CLONED',id FROM edu.quizzes WHERE id=?
                """,quizId,assessment.title()+" - "+code,baseQuiz);
        jdbc.update("""
                INSERT INTO edu.quiz3_assessment_variants(id,assessment_id,quiz_id,variant_code,order_index,shuffle_seed,variant_kind,coverage_status)
                VALUES (?,?,?,?,?,?,'SHUFFLED','COMPLIANT')
                """,variantId,assessment.id(),quizId,code,order,seed);
        List<UUID> questionIds=jdbc.query("SELECT id FROM edu.questions WHERE quiz_id=? ORDER BY order_index,id",(rs,row)->rs.getObject(1,UUID.class),baseQuiz);
        Collections.shuffle(questionIds,new Random(seed));
        for(int index=0;index<questionIds.size();index++){
            UUID sourceId=questionIds.get(index); Map<String,Object> q=jdbc.queryForMap("""
                    SELECT q.content,q.type,p.generation_job_id,p.matrix_row_id,p.matrix_custom_learning_level_id level_id,p.learning_outcome_id,
                           p.requested_asset_type,p.effective_asset_type,p.asset_fallback_reason
                    FROM edu.questions q JOIN edu.quiz3_question_provenance p ON p.question_id=q.id WHERE q.id=?
                    """,sourceId);
            List<GeneratedAnswer> answers=jdbc.query("SELECT content,is_correct,explanation FROM edu.answers WHERE question_id=? ORDER BY answer_number",
                    (rs,row)->new GeneratedAnswer(rs.getString(1),rs.getBoolean(2),rs.getString(3)),sourceId); Collections.shuffle(answers,new Random(seed^index));
            persistQuestion((UUID)q.get("generation_job_id"),quizId,new GeneratedQuestion(String.valueOf(q.get("content")),String.valueOf(q.get("type")),
                    (UUID)q.get("matrix_row_id"),(UUID)q.get("level_id"),(UUID)q.get("learning_outcome_id"),answers,null,null,
                    String.valueOf(q.get("requested_asset_type")),String.valueOf(q.get("effective_asset_type")),(String)q.get("asset_fallback_reason")),index,sourceId,null);
            jdbc.update("""
                    UPDATE edu.quiz3_question_provenance target SET qbank2_selection_mode=source.qbank2_selection_mode,
                        qbank2_content_group_ids=source.qbank2_content_group_ids,qbank2_content_group_codes=source.qbank2_content_group_codes,
                        qbank2_example_ids=source.qbank2_example_ids,qbank2_reference_snapshot=source.qbank2_reference_snapshot
                    FROM edu.quiz3_question_provenance source
                    WHERE target.question_id=(SELECT id FROM edu.questions WHERE quiz_id=? AND source_question_id=?)
                      AND source.question_id=?
                    """,quizId,sourceId,sourceId);
        }
        recalculateCoverageInternal(assessment.id(),variantId);
    }

    private List<QuestionView> questions(UUID quizId){
        return jdbc.query("""
                SELECT q.id,q.content,q.content_format,q.explanation,q.explanation_format,q.type,q.order_index,q.version,
                       p.matrix_row_id,c.title,p.learning_outcome_id,lo.prefix_number,lo.description,glo.code,
                        p.matrix_custom_learning_level_id,l.name,p.requested_asset_type,p.effective_asset_type,
                        p.qbank2_selection_mode,p.qbank2_content_group_codes::text,p.qbank2_example_ids::text
                FROM edu.questions q JOIN edu.quiz3_question_provenance p ON p.question_id=q.id
                LEFT JOIN edu.exam_matrix_rows r ON r.id=p.matrix_row_id LEFT JOIN edu.chapters c ON c.chapter_id=r.chapter_id
                LEFT JOIN edu.learning_outcomes lo ON lo.id=p.learning_outcome_id
                LEFT JOIN edu.group_learning_outcomes glo ON glo.id=lo.group_learning_outcome_id AND glo.course_id=lo.course_id
                LEFT JOIN edu.exam_matrix_custom_learning_levels l ON l.id=p.matrix_custom_learning_level_id
                WHERE q.quiz_id=? ORDER BY q.order_index,q.id
                """,(rs,row)->{
                    Object prefix=rs.getObject(12);String desc=rs.getString(13);String groupCode=rs.getString(14);
                    String lo=formatLearningOutcome(groupCode,prefix,desc);
                    UUID id=rs.getObject(1,UUID.class);return new QuestionView(id,rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),
                    rs.getInt(7),rs.getLong(8),new QuestionMetadata(rs.getObject(9,UUID.class),rs.getString(10),rs.getObject(11,UUID.class),lo,
                     rs.getObject(15,UUID.class),rs.getString(16),rs.getString(17),rs.getString(18),rs.getString(19),jsonStrings(rs.getString(20)),jsonUuids(rs.getString(21))),answers(id));},quizId);
    }

    static String formatLearningOutcome(String groupCode,Object prefix,String description){
        if(prefix==null)return description;
        String code=groupCode==null||groupCode.isBlank()?"LO"+prefix:groupCode+"."+prefix;
        return code+" - "+description;
    }

    private List<AnswerView> answers(UUID questionId){return jdbc.query("SELECT answer_number,content,content_format,is_correct FROM edu.answers WHERE question_id=? ORDER BY answer_number",
            (rs,row)->new AnswerView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)),questionId);}
    private List<String> jsonStrings(String value){if(value==null)return List.of();try{return objectMapper.readValue(value,objectMapper.getTypeFactory().constructCollectionType(List.class,String.class));}catch(Exception ex){throw new IllegalStateException("Invalid QBank2 provenance",ex);}}
    private List<UUID> jsonUuids(String value){return jsonStrings(value).stream().map(UUID::fromString).toList();}
    private void recalculateCoverageInternal(UUID assessmentId,UUID variantId){
        UUID quizId=jdbc.queryForObject("SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE id=? AND assessment_id=?",UUID.class,variantId,assessmentId);
        String snapshot=jdbc.queryForObject("""
                SELECT g.request_snapshot::text FROM edu.quiz3_assessments a
                JOIN edu.quiz3_generation_jobs g ON g.id=a.base_generation_job_id WHERE a.id=?
                """,String.class,assessmentId);
        if(snapshot==null)throw conflict("Quiz3 generation snapshot is missing");
        Map<String,Integer> expected=new LinkedHashMap<>(); JsonNode root=readJson(snapshot);
        for(JsonNode row:root.path("matrix").path("rows"))for(JsonNode cell:row.path("cells"))for(JsonNode allocation:cell.path("assetAllocations")){
            int count=allocation.path("questionCount").asInt();
            if(count>0) expected.put(allocationKey(row.path("id").asText(),cell.path("customLearningLevelId").asText(),row.path("learningOutcome").path("id").asText(),allocation.path("assetType").asText(),allocation.path("questionType").asText()),count);
        }
        Map<String,Integer> actual=new LinkedHashMap<>();
        jdbc.query("""
                SELECT p.matrix_row_id,p.matrix_custom_learning_level_id,p.learning_outcome_id,p.requested_asset_type,q.type,COUNT(*) actual
                FROM edu.quiz3_question_provenance p JOIN edu.questions q ON q.id=p.question_id WHERE q.quiz_id=?
                GROUP BY p.matrix_row_id,p.matrix_custom_learning_level_id,p.learning_outcome_id,p.requested_asset_type,q.type
                """,(org.springframework.jdbc.core.RowCallbackHandler)rs->actual.put(allocationKey(String.valueOf(rs.getObject(1)),String.valueOf(rs.getObject(2)),String.valueOf(rs.getObject(3)),rs.getString(4),rs.getString(5)),rs.getInt(6)),quizId);
        List<Map<String,Object>> differences=coverageDifferences(expected,actual);
        String status=differences.isEmpty()?"COMPLIANT":"DRIFTED";
        jdbc.update("UPDATE edu.quiz3_assessment_variants SET coverage_status=?,coverage_snapshot=CAST(? AS jsonb),updated_at=CURRENT_TIMESTAMP WHERE id=?",
                status,toJson(Map.of("status",status,"differences",differences)),variantId);
    }

    static List<Map<String,Object>> coverageDifferences(Map<String,Integer> expected,Map<String,Integer> actual){
        List<Map<String,Object>> differences=new ArrayList<>(); java.util.Set<String> keys=new java.util.LinkedHashSet<>(expected.keySet()); keys.addAll(actual.keySet());
        for(String value:keys)if(!Objects.equals(expected.getOrDefault(value,0),actual.getOrDefault(value,0)))differences.add(Map.of("cell",value,"required",expected.getOrDefault(value,0),"actual",actual.getOrDefault(value,0)));
        return differences;
    }

    private void validateQuiz(UUID quizId){
        Integer invalid=jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.questions q WHERE q.quiz_id=? AND (btrim(COALESCE(q.content,''))='' OR
                    (q.type='MULTIPLE_CHOICE' AND (SELECT COUNT(*) FROM edu.answers a WHERE a.question_id=q.id AND a.is_correct)=1 IS NOT TRUE) OR
                    (q.type='CODE_RUNNER' AND NOT EXISTS (SELECT 1 FROM edu.code_runner_configs c WHERE c.question_id=q.id AND c.contract_status='VERIFIED_EXECUTION_GOLD')))
                """,Integer.class,quizId);
        if(invalid!=null&&invalid>0) throw conflict(
                "Every MCQ needs exactly one correct answer and every Code Runner question needs a verified contract");
    }
    static void validateAnswers(List<AnswerInput> answers){if(answers==null||answers.stream().filter(AnswerInput::correct).count()!=1)throw badRequest("Exactly one answer must be correct");
        if(answers.stream().anyMatch(a->a.content()==null||a.content().isBlank()))throw badRequest("Answers cannot be blank");}
    private static void validateGeneratedAnswers(List<GeneratedAnswer> answers){if(answers==null||answers.size()!=4)throw badRequest("Quiz3 AI must return exactly four initial answers");
        if(answers.stream().filter(GeneratedAnswer::isCorrect).count()!=1)throw badRequest("Quiz3 AI must return exactly one correct answer");}
    private static String allocationKey(String row,String level,String outcome,String assetType,String questionType){return key(row,level,outcome)+"/"+assetType+"/"+questionType;}
    private UUID baseQuiz(UUID assessmentId){UUID id=jdbc.query("SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE assessment_id=? AND variant_kind='BASE'",rs->rs.next()?rs.getObject(1,UUID.class):null,assessmentId);
        if(id==null)throw conflict("Quiz 001 has not been generated");return id;}
    private MatrixSelection resolveMatrix(UUID courseId,SourceSelection selection){
        MatrixService.MatrixDetail matrix=switch(selection.mode()){
            case USE_EXISTING->matrixService.get(courseId,requiredSource(selection,"Matrix"));
            case COPY_AS_DRAFT->matrixService.copyAsDraft(courseId,requiredSource(selection,"Matrix"),new MatrixService.CopyRequest(selection.draftName()));
            case CREATE_NEW_DRAFT->{MatrixService.MatrixMode mode=requiredMatrixMode(selection);
                yield matrixService.create(courseId,new MatrixService.MatrixPayload(requiredName(selection,"Matrix"),null,
                    mode,mode==MatrixService.MatrixMode.AUTOMATIC?1:null,List.of(),List.of(),List.of(),null));}};
        return new MatrixSelection(matrix.id(),matrix.version());
    }
    private UUID requiredSource(SourceSelection source,String label){if(source==null||source.sourceId()==null)throw badRequest(label+" source is required");return source.sourceId();}
    private String requiredName(SourceSelection source,String label){String name=source==null?null:trim(source.draftName());if(name==null)throw badRequest(label+" Draft name is required");return name;}
    private Long versionOf(String table,UUID courseId,UUID id){String allowed=switch(table){case "edu.ccct_profiles", "edu.exam_matrices"->table;default->throw new IllegalArgumentException();};
        Long value=jdbc.query("SELECT version FROM "+allowed+" WHERE id=? AND course_id=?",rs->rs.next()?rs.getLong(1):null,id,courseId);if(value==null)throw badRequest("Selected source does not belong to this course");return value;}
    private AssessmentView required(UUID courseId,UUID id){AssessmentView result=jdbc.query(baseSelect()+" WHERE a.id=? AND a.course_id=?",rs->rs.next()?mapAssessment(rs):null,id,courseId);
        if(result==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Quiz3 assessment not found");return result;}
    private CurrentActor requireTeacher(UUID courseId){access.requireCourseTeacher(courseId);return signedInUserPort.findAuthenticatedUser().orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication is required"));}
    private UUID actorId(){return signedInUserPort.findAuthenticatedUser().map(CurrentActor::id).orElse(null);}
    private static MatrixService.MatrixMode requiredMatrixMode(SourceSelection selection){if(selection.matrixMode()==null)throw badRequest("Matrix mode is required when creating a Matrix draft");return MatrixService.MatrixMode.valueOf(selection.matrixMode().name());}
    private static String baseSelect(){return """
            SELECT a.id,a.course_id,a.title,a.description,a.unlimited,a.time_limit_minutes,a.pass_score,a.start_time,a.due_time,
                   a.attempt_limited,a.variant_count,a.lifecycle_status,a.current_step,a.ccct_profile_id,a.ccct_published_at,
                   a.matrix_id,a.matrix_version,m.mode,a.base_generation_job_id,a.version,a.published_at,a.created_at,a.updated_at
            FROM edu.quiz3_assessments a LEFT JOIN edu.exam_matrices m ON m.id=a.matrix_id""";}
    private static AssessmentView mapAssessment(java.sql.ResultSet rs)throws java.sql.SQLException{return new AssessmentView(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getBoolean(5),
            (Integer)rs.getObject(6),rs.getObject(7)==null?null:rs.getDouble(7),rs.getObject(8,LocalDateTime.class),rs.getObject(9,LocalDateTime.class),(Integer)rs.getObject(10),rs.getInt(11),rs.getString(12),rs.getInt(13),
            rs.getObject(14,UUID.class),rs.getObject(15,LocalDateTime.class),rs.getObject(16,UUID.class),(Long)rs.getObject(17),rs.getString(18)==null?null:MatrixMode.valueOf(rs.getString(18)),rs.getObject(19,UUID.class),rs.getLong(20),rs.getObject(21,LocalDateTime.class),rs.getObject(22,LocalDateTime.class),rs.getObject(23,LocalDateTime.class));}
    private record MatrixSelection(UUID id,long version) { }
    private static void validateConfiguration(Configuration input){if(input==null||input.title()==null||input.title().isBlank())throw badRequest("Title is required");
        if(input.variantCount()!=null&&(input.variantCount()<1||input.variantCount()>20))throw badRequest("variantCount must be between 1 and 20");
        if(input.dueTime()!=null&&input.startTime()!=null&&!input.dueTime().isAfter(input.startTime()))throw badRequest("Due time must be after start time");}
    private String toJson(Object value){try{return objectMapper.writeValueAsString(value);}catch(JsonProcessingException ex){throw new IllegalStateException(ex);}}
    private JsonNode readJson(String value){try{return objectMapper.readTree(value);}catch(JsonProcessingException ex){throw new IllegalStateException(ex);}}
    private static String sha256(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static String key(String row,String level,String lo){return row+"/"+level+"/"+lo;}
    private static String trim(String value){return value==null||value.isBlank()?null:value.trim();}
    private static String format(String value){return "TIPTAP_HTML".equals(value)?"TIPTAP_HTML":"PLAIN_TEXT";}
    private static ResponseStatusException badRequest(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
