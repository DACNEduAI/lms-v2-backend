package edu.ai.lms.core.material.api;

import java.util.UUID;

/** Safe material catalogue entry for prompt orientation; it contains no file access details. */
public record AiReadyMaterialMetadata(
        UUID id,
        String title,
        String originalFileName,
        String type
) {
}
