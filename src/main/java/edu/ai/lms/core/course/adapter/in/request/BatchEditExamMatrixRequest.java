package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchEditExamMatrixRequest {
    @Valid
    private ExamMatrixMetadataRequest metadata;

    @Valid
    @Builder.Default
    private List<CreateExamMatrixRowRequest> create = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<UpdateExamMatrixRowRequest> update = new ArrayList<>();

    @Builder.Default
    private List<UUID> delete = new ArrayList<>();
}
