package edu.ai.lms.core.course.app.service.ccct;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import edu.ai.lms.config.CorrelationIdFilter;
import edu.ai.lms.core.course.app.domain.CcctAiJob;
import edu.ai.lms.core.course.app.domain.CcctAiJobType;
import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctMode;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctTopic;
import edu.ai.lms.core.course.app.port.out.CcctAiJobPort;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;
import edu.ai.lms.core.material.api.TemporaryStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CcctAiJobProcessor {
    private final CcctAiJobPort jobPort;
    private final LoadCcctProfilePort loadProfilePort;
    private final SaveCcctProfilePort saveProfilePort;
    private final TemporaryStorageApi temporaryStorageApi;
    private final CcctMarkdownSerializer markdownSerializer;
    private final WebClient aiWebClient;

    @Async
    public void processAsync(UUID jobId) {
        if (!jobPort.claim(jobId)) {
            log.info("event=ccct.ai.skipped job_id={} reason=not_claimed", jobId);
            return;
        }
        CcctAiJob job = jobPort.findById(jobId).orElseThrow();
        long startedAt = System.nanoTime();
        log.info("event=ccct.ai.started job_id={} course_id={} profile_id={} type={}",
                job.getId(), job.getCourseId(), job.getProfileId(), job.getType());
        try {
            CcctProfile profile = requireCurrent(job);
            if (job.getType() == CcctAiJobType.PDF_TO_CUSTOM) {
                processPdf(job, profile);
            } else {
                processStructure(job, profile);
            }
            log.info("event=ccct.ai.completed job_id={} course_id={} profile_id={} type={} duration_ms={}",
                    job.getId(), job.getCourseId(), job.getProfileId(), job.getType(), elapsedMillis(startedAt));
        } catch (StaleJobException exception) {
            log.warn("event=ccct.ai.stale job_id={} course_id={} profile_id={} duration_ms={}",
                    job.getId(), job.getCourseId(), job.getProfileId(), elapsedMillis(startedAt));
            jobPort.stale(jobId, exception.getMessage());
        } catch (Exception exception) {
            log.error("event=ccct.ai.failed job_id={} course_id={} profile_id={} type={} duration_ms={} error_type={}",
                    job.getId(), job.getCourseId(), job.getProfileId(), job.getType(), elapsedMillis(startedAt),
                    exception.getClass().getSimpleName(), exception);
            jobPort.fail(jobId, safeMessage(exception));
        } finally {
            if (job.getFileKey() != null) {
                try {
                    temporaryStorageApi.delete(job.getFileKey());
                } catch (Exception ignored) {
                    // The persisted terminal job remains authoritative; stale files are cleaned separately.
                }
            }
        }
    }

    private void processPdf(CcctAiJob job, CcctProfile profile) {
        jobPort.updateStage(job.getId(), "EXTRACTING_PDF");
        String fileUrl = temporaryStorageApi.createDownloadLink(job.getFileKey(), Duration.ofMinutes(30));
        AiPdfResponse response = aiWebClient.post()
                .uri("/ccct/extract-pdf")
                .header(CorrelationIdFilter.HEADER_NAME, job.getId().toString())
                .bodyValue(new AiPdfRequest(
                        job.getCourseId(),
                        job.getProfileId(),
                        fileUrl,
                        profile.getChapters().stream()
                                .map(chapter -> new AiChapterInput(
                                        chapter.getCourseChapterId(), chapter.getTitle(), ""))
                                .toList()))
                .retrieve()
                .bodyToMono(AiPdfResponse.class)
                .block();
        if (response == null) throw new IllegalStateException("AI service returned no PDF result");

        validateChapterIds(profile, response.chapters().stream()
                .map(AiCustomChapter::courseChapterId).toList());
        Map<UUID, AiCustomChapter> byId = new HashMap<>();
        response.chapters().forEach(chapter -> byId.put(chapter.courseChapterId(), chapter));
        CcctProfile current = requireCurrent(job);
        current.getChapters().forEach(chapter -> chapter.setCustomContent(
                normalize(byId.get(chapter.getCourseChapterId()).customContent())));
        current.setStructuredCurrent(false);
        current.setLastMode(CcctMode.CUSTOM);
        current.setTemplateTitle(current.getName());
        current.setMarkdownContent(markdownSerializer.serialize(current));
        apply(job, current);
        jobPort.complete(job.getId(), response.warnings() == null ? List.of() : response.warnings());
    }

    private void processStructure(CcctAiJob job, CcctProfile profile) {
        jobPort.updateStage(job.getId(), "STRUCTURING_CONTENT");
        AiStructureResponse response = aiWebClient.post()
                .uri("/ccct/structure")
                .header(CorrelationIdFilter.HEADER_NAME, job.getId().toString())
                .bodyValue(new AiStructureRequest(
                        job.getCourseId(),
                        job.getProfileId(),
                        profile.getChapters().stream()
                                .map(chapter -> new AiChapterInput(
                                        chapter.getCourseChapterId(),
                                        chapter.getTitle(),
                                        normalize(chapter.getCustomContent())))
                                .toList()))
                .retrieve()
                .bodyToMono(AiStructureResponse.class)
                .block();
        if (response == null) throw new IllegalStateException("AI service returned no structured result");

        validateChapterIds(profile, response.chapters().stream()
                .map(AiStructuredChapter::courseChapterId).toList());
        Map<UUID, AiStructuredChapter> byId = new HashMap<>();
        response.chapters().forEach(chapter -> byId.put(chapter.courseChapterId(), chapter));
        CcctProfile current = requireCurrent(job);
        current.getChapters().forEach(chapter -> chapter.setTopics(
                byId.get(chapter.getCourseChapterId()).topics().stream()
                        .map(this::topic)
                        .toList()));
        current.setStructuredCurrent(true);
        current.setLastMode(CcctMode.STRUCTURED);
        current.setTemplateTitle(current.getName());
        current.setMarkdownContent(markdownSerializer.serialize(current));
        apply(job, current);
        jobPort.complete(job.getId(), response.warnings() == null ? List.of() : response.warnings());
    }

    private CcctTopic topic(AiTopic value) {
        return CcctTopic.builder()
                .title(value.title().trim())
                .items(value.items().stream().map(item -> CcctItem.builder()
                        .content(item.content().trim())
                        .coverageStatus(item.status())
                        .note(normalize(item.note()))
                        .build()).toList())
                .build();
    }

    private void apply(CcctAiJob job, CcctProfile profile) {
        jobPort.updateStage(job.getId(), "APPLYING_RESULT");
        if (!saveProfilePort.replaceDraft(profile, job.getExpectedVersion())) {
            throw new StaleJobException("CCCT draft changed while the AI job was running");
        }
    }

    private CcctProfile requireCurrent(CcctAiJob job) {
        CcctProfile profile = loadProfilePort.findByCourseIdAndId(job.getCourseId(), job.getProfileId())
                .orElseThrow(() -> new StaleJobException("CCCT profile no longer exists"));
        if (profile.getVersion() != job.getExpectedVersion()) {
            throw new StaleJobException("CCCT draft changed while the AI job was running");
        }
        return profile;
    }

    private void validateChapterIds(CcctProfile profile, List<UUID> responseIds) {
        Set<UUID> expected = profile.getChapters().stream()
                .map(CcctChapter::getCourseChapterId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> actual = new HashSet<>(responseIds);
        if (responseIds.size() != expected.size() || !actual.equals(expected)) {
            throw new IllegalStateException("AI result must contain every Course Chapter exactly once");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "CCCT AI processing failed"
                : message.substring(0, Math.min(message.length(), 1000));
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private static final class StaleJobException extends RuntimeException {
        private StaleJobException(String message) {
            super(message);
        }
    }

    private record AiChapterInput(UUID courseChapterId, String title, String customContent) { }
    private record AiPdfRequest(UUID courseId, UUID profileId, String fileUrl, List<AiChapterInput> chapters) { }
    private record AiCustomChapter(UUID courseChapterId, String customContent, double confidence) { }
    private record AiPdfResponse(List<AiCustomChapter> chapters, List<String> warnings) { }
    private record AiStructureRequest(UUID courseId, UUID profileId, List<AiChapterInput> chapters) { }
    private record AiItem(String content, CcctCoverageStatus status, String note) { }
    private record AiTopic(String title, List<AiItem> items) { }
    private record AiStructuredChapter(UUID courseChapterId, List<AiTopic> topics) { }
    private record AiStructureResponse(List<AiStructuredChapter> chapters, List<String> warnings) { }
}
