package edu.ai.lms.core.course.adapter.in.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyLearningOutcomeResponse {
    private UUID id;
    private String code;
    private Integer prefixNumber;
    private Integer sortOrder;
    private String displayCode;
    private String description;
    private UUID courseId;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime updatedAt;
}
