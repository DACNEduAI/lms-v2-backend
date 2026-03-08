package edu.ai.lms.core.course.app.port.in.usecase;

import java.util.List;
import java.util.UUID;

import edu.ai.lms.core.course.adapter.in.response.GetChapterResponse;

public interface GetAllChaptersUseCase {
    List<GetChapterResponse> getAllChapters(UUID courseId);
}
