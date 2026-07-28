package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;

public record CcctTemplateResponse(String title, List<CcctChapterResponse> chapters) {
}
