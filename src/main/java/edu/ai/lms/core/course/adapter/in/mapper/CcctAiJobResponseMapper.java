package edu.ai.lms.core.course.adapter.in.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.in.response.CcctAiJobResponse;
import edu.ai.lms.core.course.app.domain.CcctAiJob;

@Component
public class CcctAiJobResponseMapper {
    public CcctAiJobResponse toResponse(CcctAiJob job) {
        return new CcctAiJobResponse(
                job.getId(),
                job.getCourseId(),
                job.getProfileId(),
                job.getType(),
                job.getStatus(),
                job.getStage(),
                job.getExpectedVersion(),
                List.copyOf(job.getWarnings()),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getCompletedAt());
    }
}
