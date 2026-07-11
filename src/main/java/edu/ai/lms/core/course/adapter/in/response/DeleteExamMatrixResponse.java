package edu.ai.lms.core.course.adapter.in.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after successfully deleting an exam matrix.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteExamMatrixResponse {

    private UUID matrixId;
    private String message;
    private boolean success;
}
