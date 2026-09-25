package edu.ai.lms.core.course.app.domain;

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
public class CcctItem {
    private UUID id;
    private UUID topicId;
    private String contentCode;
    private String content;
    private int orderIndex;
}
