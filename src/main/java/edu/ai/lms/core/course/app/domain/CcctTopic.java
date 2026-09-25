package edu.ai.lms.core.course.app.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
public class CcctTopic {
    private UUID id;
    private UUID chapterId;
    /** Teacher-defined identifier used by Matrix content codes. */
    private String groupCode;
    private String title;
    private int orderIndex;

    @Builder.Default
    private List<CcctItem> items = new ArrayList<>();
}
