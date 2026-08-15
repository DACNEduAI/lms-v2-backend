package edu.ai.lms.core.quiz3;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentUploadService;
import lombok.RequiredArgsConstructor;

/** Quiz3 adapter over the existing question specialists with Quiz3-owned persistence. */
@Service
@RequiredArgsConstructor
public class Quiz3QuestionAiService {
    private final JdbcTemplate jdbc;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final ExamMatrixAccess access;
    private final LoadSignedInUserPort actors;
    private final RichContentUploadService uploads;
    private final RichContentAssetRepository richAssets;

    @Transactional
    public Quiz3Models.AiDraft refine(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            Quiz3Models.AiRequest request) {
        CurrentActor actor = authorize(courseId, assessmentId, variantId, questionId);
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("Question-AI prompt is required");
        }
        String mode = request.mode() == null ? "TEXT" : request.mode().toUpperCase();
        if (!List.of("TEXT", "LATEX", "SVG").contains(mode)) {
            throw new IllegalArgumentException("Quiz3 Question-AI mode must be TEXT, LATEX, or SVG");
        }
        Map<String, Object> question = jdbc.queryForMap("SELECT content,content_format,version FROM edu.questions WHERE id=?", questionId);
        long version = ((Number) question.get("version")).longValue();
        if (version != request.expectedQuestionVersion()) throw new IllegalStateException("Question changed; reload before using AI");
        String content = request.currentContent() == null ? String.valueOf(question.get("content")) : request.currentContent();
        String format = "TIPTAP_HTML".equals(request.currentContentFormat()) ? "TIPTAP_HTML" : String.valueOf(question.get("content_format"));
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_working_drafts
                    (question_id,assessment_id,variant_id,base_question_version,content,content_format)
                VALUES (?,?,?,?,?,?) ON CONFLICT(question_id) DO UPDATE SET content=EXCLUDED.content,
                    content_format=EXCLUDED.content_format,updated_at=CURRENT_TIMESTAMP
                """, questionId, assessmentId, variantId, version, content, format);
        UUID jobId = UUID.randomUUID();
        String payload = specialistPayload(mode, request.prompt(), content, format);
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_jobs
                    (id,assessment_id,variant_id,question_id,requested_by,mode,status,phase,prompt,request_payload,
                     base_question_version,attempt_count,action_count,started_at)
                VALUES (?,?,?,?,?,?,'RUNNING',?,?,CAST(? AS jsonb),?,1,1,CURRENT_TIMESTAMP)
                """, jobId, assessmentId, variantId, questionId, actor.id(), mode, mode,
                request.prompt().trim(), payload, version);
        try {
            JsonNode response = aiWebClient.post().uri(specialistPath(mode))
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(objectMapper.readTree(payload)).retrieve()
                    .bodyToMono(JsonNode.class).block(Duration.ofMinutes(2));
            SpecialistResult result = applySpecialistResult(mode, response, content, format,
                    assessmentId, questionId, actor.id(), jobId, request.prompt());
            String after = result.content();
            String afterFormat = result.format();
            int next = jdbc.queryForObject("SELECT COALESCE(MAX(change_index),0)+1 FROM edu.quiz3_question_ai_change_sets WHERE question_id=?", Integer.class, questionId);
            jdbc.update("DELETE FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND change_index>=?", questionId, next);
            jdbc.update("""
                    INSERT INTO edu.quiz3_question_ai_change_sets
                        (id,question_id,job_id,change_index,before_content,before_content_format,after_content,after_content_format)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, UUID.randomUUID(), questionId, jobId, next, content, format, after, afterFormat);
            jdbc.update("""
                    UPDATE edu.quiz3_question_ai_working_drafts SET content=?,content_format=?,applied_change_index=?,
                        updated_at=CURRENT_TIMESTAMP WHERE question_id=?
                    """, after, afterFormat, next, questionId);
            jdbc.update("""
                    UPDATE edu.quiz3_question_ai_jobs SET status='APPLIED_TO_WORKING_DRAFT',phase='DONE',
                        completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, jobId);
            return draft(courseId, assessmentId, variantId, questionId);
        } catch (Exception ex) {
            jdbc.update("""
                    UPDATE edu.quiz3_question_ai_jobs SET status='FAILED',phase='DONE',error_code='SPECIALIST_FAILED',
                        error_message=?,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, String.valueOf(ex.getMessage()), jobId);
            throw new IllegalStateException("Question-AI specialist failed", ex);
        }
    }

    public Quiz3Models.AiDraft draft(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId) {
        authorize(courseId, assessmentId, variantId, questionId);
        return jdbc.query("""
                SELECT d.base_question_version,d.content,d.content_format,d.applied_change_index,
                       COALESCE((SELECT MAX(c.change_index) FROM edu.quiz3_question_ai_change_sets c WHERE c.question_id=d.question_id),0)
                FROM edu.quiz3_question_ai_working_drafts d WHERE d.question_id=?
                """, rs -> {
            if (!rs.next()) return null;
            int applied=rs.getInt(4),latest=rs.getInt(5);
            return new Quiz3Models.AiDraft(questionId,rs.getLong(1),rs.getString(2),rs.getString(3),applied,latest,applied>0,applied<latest);
        }, questionId);
    }

    @Transactional
    public Quiz3Models.AiDraft move(UUID courseId,UUID assessmentId,UUID variantId,UUID questionId,boolean redo){
        Quiz3Models.AiDraft current=draft(courseId,assessmentId,variantId,questionId);
        if(current==null)throw new IllegalStateException("Question-AI working draft does not exist");
        int target=redo?current.appliedChangeIndex()+1:current.appliedChangeIndex()-1;
        if(target<0||target>current.latestChangeIndex())throw new IllegalStateException("Question-AI history cannot move further");
        String content;String format;
        if(target==0){var first=jdbc.queryForMap("SELECT before_content,before_content_format FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND change_index=1",questionId);content=String.valueOf(first.get("before_content"));format=String.valueOf(first.get("before_content_format"));}
        else{var change=jdbc.queryForMap("SELECT after_content,after_content_format FROM edu.quiz3_question_ai_change_sets WHERE question_id=? AND change_index=?",questionId,target);content=String.valueOf(change.get("after_content"));format=String.valueOf(change.get("after_content_format"));}
        jdbc.update("UPDATE edu.quiz3_question_ai_working_drafts SET content=?,content_format=?,applied_change_index=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=?",content,format,target,questionId);
        return draft(courseId,assessmentId,variantId,questionId);
    }

    private CurrentActor authorize(UUID courseId,UUID assessmentId,UUID variantId,UUID questionId){access.requireCourseTeacher(courseId);
        Integer count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.quiz3_assessments a JOIN edu.quiz3_assessment_variants v ON v.assessment_id=a.id
                JOIN edu.questions q ON q.quiz_id=v.quiz_id WHERE a.id=? AND a.course_id=? AND v.id=? AND q.id=?
                """,Integer.class,assessmentId,courseId,variantId,questionId);if(count==null||count!=1)throw new IllegalArgumentException("Quiz3 question context is invalid");
        return actors.findAuthenticatedUser().orElseThrow();}
    private String json(Object value){try{return objectMapper.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private String specialistPayload(String mode,String prompt,String content,String format){
        return switch(mode){
            case "LATEX"->json(Map.of("operation","CREATE","prompt",prompt,"questionContent",content,
                    "anchor",Map.of("kind","END")));
            case "SVG"->json(Map.of("operation","CREATE","prompt",prompt,"questionContent",content,"altText",prompt));
            default->json(Map.of("prompt",prompt,"content",content,"contentFormat",format,"protectedArtifactTokens",List.of()));
        };}
    private static String specialistPath(String mode){return switch(mode){case "LATEX"->"/quiz-gen/questions/latex/generate";
        case "SVG"->"/quiz-gen/questions/svg/generate";default->"/quiz-gen/questions/text/refine";};}
    private SpecialistResult applySpecialistResult(String mode,JsonNode response,String content,String format,
            UUID assessmentId,UUID questionId,UUID actorId,UUID jobId,String prompt){
        if(response==null)throw new IllegalStateException("Question-AI returned no response");
        if("TEXT".equals(mode)){if(!response.has("content"))throw new IllegalStateException("Question-AI returned no content");
            return new SpecialistResult(response.path("content").asText(),response.path("contentFormat").asText(format));}
        Document document="TIPTAP_HTML".equals(format)?Jsoup.parseBodyFragment(content):Jsoup.parseBodyFragment("");
        if(!"TIPTAP_HTML".equals(format))document.body().appendElement("p").text(content);
        UUID artifactId=UUID.randomUUID();String source=response.path("sourceCode").asText();
        if(source.isBlank())throw new IllegalStateException("Question-AI returned an empty artifact");
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_artifacts(id,assessment_id,question_id,artifact_type,current_revision,created_by)
                VALUES (?,?,?,?,1,?)
                """,artifactId,assessmentId,questionId,mode,actorId);
        String visualKind=response.path("visualKind").isMissingNode()?null:response.path("visualKind").asText(null);
        String generator=response.path("generatorUsed").isMissingNode()?null:response.path("generatorUsed").asText(null);
        String spec=response.path("spec").isMissingNode()||response.path("spec").isNull()?null:response.path("spec").toString();
        jdbc.update("""
                INSERT INTO edu.quiz3_question_ai_artifact_revisions
                    (artifact_id,revision,job_id,source_code,source_hash,latex_mode,visual_kind,generator,structured_spec,prompt,critic_result)
                VALUES (?,1,?,?,?,?,?,?,CAST(? AS jsonb),?,CAST(? AS jsonb))
                """,artifactId,jobId,source,sha256(source),"LATEX".equals(mode)?response.path("mode").asText("INLINE"):null,
                visualKind,generator,spec,prompt,response.path("critic").isMissingNode()||response.path("critic").isNull()?null:response.path("critic").toString());
        if("LATEX".equals(mode)){
            boolean block="BLOCK".equals(response.path("mode").asText());
            document.body().appendElement(block?"div":"span").attr("data-type",block?"block-math":"inline-math")
                    .attr("data-latex",source).attr("data-ai-artifact-id",artifactId.toString()).attr("data-ai-artifact-type","LATEX");
        }else{
            RichContentUploadService.UploadedAsset stored=uploads.uploadAiSvg(source);uploads.cleanupAfterRollback(List.of(stored));
            richAssets.save(new RichContentAsset(UUID.randomUUID(),null,null,questionId,stored.storageKey(),stored.mediaType(),
                    stored.byteSize(),stored.sha256(),null,null,null));
            document.body().appendElement("img").attr("src",stored.publicUrl()).attr("alt",response.path("altText").asText(prompt))
                    .attr("data-ai-artifact-id",artifactId.toString()).attr("data-ai-artifact-type","SVG");
        }
        return new SpecialistResult(document.body().html(),"TIPTAP_HTML");}
    private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private record SpecialistResult(String content,String format){}
}
