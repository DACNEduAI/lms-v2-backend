package edu.ai.lms.core.course.adapter.in.response;

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
public class DeleteChapterResponse {
    private UUID courseId;
    private UUID chapterId;
    private String message;
    private Boolean success;
}
