package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.Chapter;

public interface LoadChapterPort {
    Optional<Chapter> findByKey(UUID courseId, Long chapterNumber);
    List<Chapter> findAllByCourseId(UUID courseId);
}
