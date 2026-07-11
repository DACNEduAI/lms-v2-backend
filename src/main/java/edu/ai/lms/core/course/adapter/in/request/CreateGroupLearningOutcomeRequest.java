package edu.ai.lms.core.course.adapter.in.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupLearningOutcomeRequest {

    private String code;

    private String name;

    private String description;

    private List<UUID> learningOutcomeIds;
}
