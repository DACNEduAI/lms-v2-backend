package edu.ai.lms.core.course.app.service.ccct;

import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.app.domain.CcctAiJob;
import edu.ai.lms.core.course.app.domain.CcctAiJobStatus;
import edu.ai.lms.core.course.app.domain.CcctAiJobType;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.exception.CcctAiJobNotFoundException;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.exception.CcctPayloadTooLargeException;
import edu.ai.lms.core.course.app.exception.CcctValidationException;
import edu.ai.lms.core.course.app.port.out.CcctAiJobPort;
import edu.ai.lms.core.material.api.TemporaryStorageApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CcctAiJobService {
    private static final int MAX_PDF_BYTES = 25 * 1024 * 1024;
    private final CcctAccess access;
    private final CcctProfiles profiles;
    private final CcctAiJobPort jobPort;
    private final TemporaryStorageApi temporaryStorageApi;
    private final CcctAiJobProcessor processor;

    public CcctAiJob startPdf(
            UUID courseId,
            UUID profileId,
            long version,
            String fileName,
            String contentType,
            byte[] bytes) {
        access.requireCourseTeacher(courseId);
        CcctProfile profile = requireEditable(courseId, profileId, version);
        validatePdf(fileName, contentType, bytes);

        UUID jobId = UUID.randomUUID();
        String fileKey = "ccct-ai/" + courseId + "/" + jobId + ".pdf";
        temporaryStorageApi.upload(fileKey, bytes, "application/pdf");
        try {
            CcctAiJob job = jobPort.create(CcctAiJob.builder()
                    .id(jobId)
                    .courseId(courseId)
                    .profileId(profileId)
                    .type(CcctAiJobType.PDF_TO_CUSTOM)
                    .status(CcctAiJobStatus.QUEUED)
                    .stage("QUEUED")
                    .expectedVersion(profile.getVersion())
                    .fileKey(fileKey)
                    .build());
            processor.processAsync(job.getId());
            return job;
        } catch (RuntimeException exception) {
            temporaryStorageApi.delete(fileKey);
            throw exception;
        }
    }

    public CcctAiJob startStructure(UUID courseId, UUID profileId, long version) {
        access.requireCourseTeacher(courseId);
        CcctProfile profile = requireEditable(courseId, profileId, version);
        CcctAiJob job = jobPort.create(CcctAiJob.builder()
                .courseId(courseId)
                .profileId(profileId)
                .type(CcctAiJobType.CUSTOM_TO_STRUCTURED)
                .status(CcctAiJobStatus.QUEUED)
                .stage("QUEUED")
                .expectedVersion(profile.getVersion())
                .build());
        processor.processAsync(job.getId());
        return job;
    }

    public CcctAiJob get(UUID courseId, UUID profileId, UUID jobId) {
        access.requireCourseTeacher(courseId);
        CcctAiJob job = jobPort.findById(jobId)
                .orElseThrow(() -> new CcctAiJobNotFoundException("CCCT AI job not found: " + jobId));
        if (!job.getCourseId().equals(courseId) || !job.getProfileId().equals(profileId)) {
            throw new CcctAiJobNotFoundException("CCCT AI job not found: " + jobId);
        }
        return job;
    }

    private CcctProfile requireEditable(UUID courseId, UUID profileId, long version) {
        CcctProfile profile = profiles.require(courseId, profileId);
        if (profile.getFormatVersion() != 2) {
            throw new CcctConflictException("Legacy CCCT profiles are read-only");
        }
        if (profile.getStatus() != CcctProfileStatus.DRAFT) {
            throw new CcctConflictException("Only DRAFT profiles can run CCCT AI jobs");
        }
        if (profile.getVersion() != version) {
            throw new CcctConflictException("CCCT draft changed before the AI job started");
        }
        return profile;
    }

    private void validatePdf(String fileName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new CcctValidationException("PDF file cannot be empty");
        }
        boolean pdfName = fileName != null && fileName.toLowerCase().endsWith(".pdf");
        boolean pdfType = "application/pdf".equalsIgnoreCase(contentType);
        if (!pdfName && !pdfType) {
            throw new CcctValidationException("Only PDF files are supported");
        }
        if (bytes.length > MAX_PDF_BYTES) {
            throw new CcctPayloadTooLargeException("CCCT PDF cannot exceed 25 MB");
        }
    }
}
