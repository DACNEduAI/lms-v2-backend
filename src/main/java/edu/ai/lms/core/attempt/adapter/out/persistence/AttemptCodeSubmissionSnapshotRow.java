package edu.ai.lms.core.attempt.adapter.out.persistence;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AttemptCodeSubmissionSnapshotRow {
    private UUID questionId;
    private UUID submissionId;
}
