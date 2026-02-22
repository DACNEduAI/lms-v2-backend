package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.Chapter;

public interface LoadChapterPort {
    Optional<Chapter> findById(UUID chapterId);
    Optional<Chapter> findByIdWithoutMaterials(UUID chapterId);
}
