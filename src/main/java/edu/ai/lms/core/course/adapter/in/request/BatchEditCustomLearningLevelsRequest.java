package edu.ai.lms.core.course.adapter.in.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BatchEditCustomLearningLevelsRequest {
    @Valid
    private List<BatchCreateCustomLearningLevelRequest> create = new ArrayList<>();

    @Valid
    private List<UpdateCustomLearningLevelRequest> update = new ArrayList<>();

    private List<UUID> delete = new ArrayList<>();
}
