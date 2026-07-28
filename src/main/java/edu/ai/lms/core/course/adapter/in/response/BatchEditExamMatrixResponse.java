package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;

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
public class BatchEditExamMatrixResponse {
    private List<CreatedRowMappingResponse> createdRowMappings;
    private List<CreatedCustomLearningLevelMappingResponse> createdLevelMappings;
    private ExamMatrixResponse matrix;
}
