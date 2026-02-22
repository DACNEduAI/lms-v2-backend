package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.Chapter;

public interface SaveChapterPort {
    Chapter save(Chapter chapter);
}
