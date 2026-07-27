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
public class CreatedRowMappingResponse {
    private String clientTempId;
    private UUID rowId;
}
