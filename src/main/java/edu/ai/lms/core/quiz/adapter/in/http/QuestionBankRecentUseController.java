package edu.ai.lms.core.quiz.adapter.in.http;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses/{courseId}/question-bank/recently-used")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBankRecentUseController {
    private final AssessmentService service;

    @PostMapping("/reset")
    public ResetResult reset(@PathVariable UUID courseId, @RequestBody ResetRequest request) {
        return new ResetResult(service.resetRecentlyUsed(courseId, request.questionIds()));
    }

    public record ResetRequest(List<UUID> questionIds) {
    }

    public record ResetResult(int resetCount) {
    }
}
