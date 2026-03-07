package edu.ai.lms.core.material.app.port.out.persistence;

import edu.ai.lms.core.material.app.domain.Material;

import java.util.UUID;

public interface CreateMaterialPort {

    /**
     * Saves a new material linked only to a course.
     */
    Material createForCourse(UUID courseId, Material material);

    /**
     * Saves a new material linked to both a course and a specific chapter.
     */
    Material createForChapter(UUID courseId, Long chapterNumber, Material material);
}
