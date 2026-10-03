package edu.ai.lms.core.quiz3.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import edu.ai.lms.core.asset.adapter.out.persistence.AssetGenerationJobMapper;
import edu.ai.lms.core.asset.adapter.out.persistence.AssetGenerationJobRow;
import edu.ai.lms.core.asset.app.domain.Asset;
import edu.ai.lms.core.asset.app.domain.ImageAsset;
import edu.ai.lms.core.asset.app.port.out.AssetRepository;
import edu.ai.lms.core.asset.app.service.AssetImageGenerationService;
import edu.ai.lms.core.asset.app.service.AssetVectorOutboxService;
import edu.ai.lms.core.material.api.CourseAssetStorageApi;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;
import edu.ai.lms.core.quiz3.app.domain.Quiz3RichContentAsset;
import lombok.RequiredArgsConstructor;

/** Handles each visual independently after text callback; no image failure invalidates the question text. */
@Component
@RequiredArgsConstructor
public class Quiz3VisualCoordinator {
    private final JdbcTemplate jdbc;
    private final AssetRepository assets;
    private final AssetGenerationJobMapper jobs;
    private final AssetImageGenerationService generation;
    private final AssetVectorOutboxService vectorOutbox;
    private final CourseAssetStorageApi courseStorage;
    private final Quiz3RichContentUploadService uploads;
    private final Quiz3RichContentAssetRepository richAssets;
    private final Quiz3RichContentNormalizer normalizer;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelayString = "${quiz3.visual.dispatch-delay-ms:2000}")
    public void dispatch() {
        List<UUID> ids=jdbc.query("SELECT question_id FROM edu.quiz3_question_visuals WHERE status IN ('PENDING','PROCESSING') ORDER BY created_at LIMIT 20",(rs,row)->rs.getObject(1,UUID.class));
        ids.forEach(id -> { try { transactions.executeWithoutResult(status -> advance(id)); } catch (RuntimeException ignored) { /* transient dependency; retry next tick */ } });
    }

    @Transactional
    public void advance(UUID questionId) {
        Row row=jdbc.query("""
                SELECT v.question_id,v.generation_job_id,g.course_id,v.placement,v.answer_index,v.alt_text,v.resolution_action,
                       v.selected_asset_id,v.generated_asset_id,v.asset_generation_job_id,v.status,v.generation_prompt
                FROM edu.quiz3_question_visuals v JOIN edu.quiz3_generation_jobs g ON g.id=v.generation_job_id WHERE v.question_id=? FOR UPDATE
                """,rs->rs.next()?new Row(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getString(4),(Integer)rs.getObject(5),rs.getString(6),rs.getString(7),rs.getObject(8,UUID.class),rs.getObject(9,UUID.class),rs.getObject(10,UUID.class),rs.getString(11),rs.getString(12)):null,questionId);
        if(row==null) return;
        if("PENDING".equals(row.status())) {
            ImageAsset source=row.selectedAssetId()==null?null:assets.findImage(row.courseId(),row.selectedAssetId()).orElse(null);
            if("REUSE_ASSET".equals(row.action()) && reusable(source)) { snapshot(row,source,source.asset().id()); return; }
            String prompt=row.generationPrompt();
            if ("REUSE_ASSET".equals(row.action()) && (prompt == null || prompt.isBlank())) prompt=fallbackPrompt(row.questionId());
            if(prompt==null||prompt.isBlank()) { fail(row,"Image generation prompt is missing"); return; }
            AssetImageGenerationService.Accepted created=generation.startQuiz3Visual(row.courseId(),prompt);
            jdbc.update("UPDATE edu.quiz3_question_visuals SET status='PROCESSING',generated_asset_id=?,asset_generation_job_id=?,attempt_count=1,updated_at=CURRENT_TIMESTAMP WHERE question_id=?",created.assetId(),created.jobId(),row.questionId());
            return;
        }
        AssetGenerationJobRow job=row.assetJobId()==null?null:jobs.find(row.courseId(),row.assetJobId());
        if(job==null) { fail(row,"Image generation job is missing"); return; }
        if("FAILED".equals(job.getStatus())) { fail(row,job.getErrorMessage()); return; }
        if(!"READY_FOR_REVIEW".equals(job.getStatus())) return;
        ImageAsset image=generation.publishQuiz3Visual(row.courseId(),row.generatedAssetId());
        vectorOutbox.enqueue(image.asset().id());
        snapshot(row,image,image.asset().id());
    }

    private static boolean reusable(ImageAsset image) { return image!=null && image.asset().publicationStatus()==Asset.PublicationStatus.PUBLISHED && image.asset().uploadStatus()==Asset.UploadStatus.UPLOADED; }
    private void snapshot(Row row,ImageAsset source,UUID sourceAssetId) {
        Quiz3RichContentUploadService.UploadedAsset copy=uploads.snapshotCourseAsset(courseStorage.read(source.storageKey()),source.contentType());
        uploads.cleanupAfterRollback(List.of(copy));
        String image="<img src=\""+escape(copy.publicUrl())+"\" alt=\""+escape(row.altText())+"\" data-image-layout=\"block\" />";
        var allowedImageUrls=uploads.publicUrls(richAssets.findByQuestionId(row.questionId()),List.of(copy));
        if("QUESTION".equals(row.placement())) {
            ContentRow current=jdbc.query("SELECT content,content_format FROM edu.questions WHERE id=?",
                    rs->rs.next()?new ContentRow(rs.getString(1),rs.getString(2),null):null,row.questionId());
            if(current==null) throw new IllegalStateException("Quiz3 visual question is missing");
            var normalized=normalizer.appendOwnedImage(current.content(),current.contentFormat(),image,allowedImageUrls::contains);
            jdbc.update("UPDATE edu.questions SET content=?,content_format=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    normalized.content(),normalized.contentFormat().name(),row.questionId());
        } else {
            ContentRow current=jdbc.query("""
                    SELECT content,content_format,answer_number FROM edu.answers WHERE question_id=?
                    ORDER BY answer_number OFFSET ? LIMIT 1
                    """,rs->rs.next()?new ContentRow(rs.getString(1),rs.getString(2),rs.getLong(3)):null,row.questionId(),row.answerIndex());
            if(current==null) throw new IllegalStateException("Quiz3 visual answer is missing");
            var normalized=normalizer.appendOwnedImage(current.content(),current.contentFormat(),image,allowedImageUrls::contains);
            jdbc.update("UPDATE edu.answers SET content=?,content_format=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND answer_number=?",
                    normalized.content(),normalized.contentFormat().name(),row.questionId(),current.answerNumber());
        }
        richAssets.save(new Quiz3RichContentAsset(UUID.randomUUID(),row.questionId(),copy.storageKey(),copy.mediaType(),copy.byteSize(),copy.sha256(),sourceAssetId,null,null,null));
        jdbc.update("UPDATE edu.quiz3_question_visuals SET status='READY',error_message=NULL,updated_at=CURRENT_TIMESTAMP WHERE question_id=?",row.questionId());
        jdbc.update("UPDATE edu.quiz3_question_provenance SET effective_asset_type='IMAGE',asset_fallback_reason=NULL WHERE question_id=?",row.questionId());
        finalizeJob(row.jobId());
    }
    private void fail(Row row,String reason) {
        String message=reason==null||reason.isBlank()?"Image generation failed after retries":reason;
        jdbc.update("UPDATE edu.quiz3_question_visuals SET status='FAILED',error_message=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=?",message.substring(0,Math.min(1000,message.length())),row.questionId());
        jdbc.update("UPDATE edu.quiz3_question_provenance SET effective_asset_type='TEXT',asset_fallback_reason='IMAGE_GENERATION_FAILED_AFTER_RETRIES' WHERE question_id=?",row.questionId());
        finalizeJob(row.jobId());
    }
    private String fallbackPrompt(UUID questionId) {
        return jdbc.queryForObject("""
                SELECT 'Create a new accurate educational image from this question only. Visual brief: ' || v.visual_brief || '. Question: ' || q.content
                FROM edu.quiz3_question_visuals v JOIN edu.questions q ON q.id=v.question_id WHERE v.question_id=?
                """,String.class,questionId);
    }
    private void finalizeJob(UUID jobId) {
        int total=jdbc.queryForObject("SELECT COUNT(*) FROM edu.quiz3_question_visuals WHERE generation_job_id=?",Integer.class,jobId);
        int terminal=jdbc.queryForObject("SELECT COUNT(*) FROM edu.quiz3_question_visuals WHERE generation_job_id=? AND status IN ('READY','FAILED')",Integer.class,jobId);
        if(total==0||terminal<total)return;
        int failed=jdbc.queryForObject("SELECT COUNT(*) FROM edu.quiz3_question_visuals WHERE generation_job_id=? AND status='FAILED'",Integer.class,jobId);
        jdbc.update("UPDATE edu.quiz3_generation_jobs SET status=?,phase='DONE',processed_visual_count=?,failed_visual_count=?,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'",failed==0?"COMPLETED":"COMPLETED_WITH_WARNINGS",terminal,failed,jobId);
        jdbc.update("UPDATE edu.quiz3_assessments SET lifecycle_status='REVIEW_PENDING',current_step=GREATEST(current_step,6),version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=(SELECT assessment_id FROM edu.quiz3_generation_jobs WHERE id=?)",jobId);
    }
    private static String escape(String value) { return value==null?"":value.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;"); }
    private record ContentRow(String content,String contentFormat,Long answerNumber) { }
    private record Row(UUID questionId,UUID jobId,UUID courseId,String placement,Integer answerIndex,String altText,String action,UUID selectedAssetId,UUID generatedAssetId,UUID assetJobId,String status,String generationPrompt) { }
}
