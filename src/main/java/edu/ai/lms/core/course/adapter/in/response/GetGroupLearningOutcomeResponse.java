package edu.ai.lms.core.course.adapter.in.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetGroupLearningOutcomeResponse {

    private UUID id;
    private String name;
    private String code;
    private Integer sortOrder;
    private String displayCode;
    private String description;
    private UUID courseId;

    @Builder.Default
    private List<GetLearningOutcomeResponse> learningOutcomes = new ArrayList<>();
}
