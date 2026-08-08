package edu.ai.lms.core.course.app.service.ccct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.app.domain.CcctAiJob;
import edu.ai.lms.core.course.app.domain.CcctAiJobStatus;
import edu.ai.lms.core.course.app.domain.CcctAiJobType;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.exception.CcctValidationException;
import edu.ai.lms.core.course.app.port.out.CcctAiJobPort;
import edu.ai.lms.core.material.api.TemporaryStorageApi;

@ExtendWith(MockitoExtension.class)
class CcctAiJobServiceUT {
    @Mock private CcctAccess access;
    @Mock private CcctProfiles profiles;
    @Mock private CcctAiJobPort jobPort;
    @Mock private TemporaryStorageApi temporaryStorageApi;
    @Mock private CcctAiJobProcessor processor;
    @InjectMocks private CcctAiJobService service;

    @Test
    void createsStructureJobForCurrentV2Draft() {
        UUID courseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(profiles.require(courseId, profileId)).thenReturn(draft(profileId, 7));
        when(jobPort.create(any())).thenAnswer(invocation -> {
            CcctAiJob job = invocation.getArgument(0);
            job.setId(jobId);
            return job;
        });

        CcctAiJob result = service.startStructure(courseId, profileId, 7);

        assertThat(result.getType()).isEqualTo(CcctAiJobType.CUSTOM_TO_STRUCTURED);
        assertThat(result.getStatus()).isEqualTo(CcctAiJobStatus.QUEUED);
        assertThat(result.getExpectedVersion()).isEqualTo(7);
        verify(processor).processAsync(jobId);
    }

    @Test
    void rejectsStaleProfileBeforeCreatingAJob() {
        UUID courseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(profiles.require(courseId, profileId)).thenReturn(draft(profileId, 8));

        assertThatThrownBy(() -> service.startStructure(courseId, profileId, 7))
                .isInstanceOf(CcctConflictException.class)
                .hasMessageContaining("changed");
        verify(jobPort, never()).create(any());
    }

    @Test
    void validatesPdfBeforeUploadingTemporaryObject() {
        UUID courseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(profiles.require(courseId, profileId)).thenReturn(draft(profileId, 1));

        assertThatThrownBy(() -> service.startPdf(
                courseId, profileId, 1, "notes.txt", "text/plain", new byte[] { 1 }))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("PDF");
        verify(temporaryStorageApi, never()).upload(any(), any(), any());
    }

    @Test
    void hidesJobBelongingToAnotherProfile() {
        UUID courseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(jobPort.findById(jobId)).thenReturn(java.util.Optional.of(CcctAiJob.builder()
                .id(jobId)
                .courseId(courseId)
                .profileId(UUID.randomUUID())
                .build()));

        assertThatThrownBy(() -> service.get(courseId, profileId, jobId))
                .isInstanceOf(edu.ai.lms.core.course.app.exception.CcctAiJobNotFoundException.class);
    }

    private CcctProfile draft(UUID id, long version) {
        return CcctProfile.builder()
                .id(id)
                .formatVersion(2)
                .status(CcctProfileStatus.DRAFT)
                .version(version)
                .build();
    }
}
