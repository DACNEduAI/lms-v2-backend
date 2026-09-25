package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

public record CcctTopicResponse(UUID id, String groupCode, String title, List<CcctItemResponse> items) {
}
