package edu.ai.lms.core.material.app.port.in.command;

import edu.ai.lms.core.material.app.domain.Material;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateMaterialForChapterCommand(UUID courseId, Long chapterNumber, Material material) {
}
