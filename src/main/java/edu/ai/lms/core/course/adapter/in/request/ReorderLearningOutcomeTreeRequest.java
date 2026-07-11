package edu.ai.lms.core.course.adapter.in.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ReorderLearningOutcomeTreeRequest {

    @NotEmpty(message = "Groups must not be empty")
    @Valid
    private List<GroupItem> groups;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupItem {
        @NotNull(message = "Group learning outcome ID cannot be null")
        private UUID groupLearningOutcomeId;

        @Builder.Default
        private List<UUID> learningOutcomeIds = List.of();
    }
}
