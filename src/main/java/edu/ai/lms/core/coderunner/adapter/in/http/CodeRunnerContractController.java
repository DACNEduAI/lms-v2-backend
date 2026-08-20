package edu.ai.lms.core.coderunner.adapter.in.http;

import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.adapter.in.http.response.CodeRunnerVerificationResponse;
import edu.ai.lms.core.coderunner.app.service.CodeRunnerContractService;
import edu.ai.lms.core.coderunner.app.service.TeacherCodeSubmissionService;
import edu.ai.lms.core.coderunner.app.domain.TeacherCodeSubmissionView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/code-runner")
public class CodeRunnerContractController {

    private final CodeRunnerContractService service;
    private final TeacherCodeSubmissionService submissions;

    @PutMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ExecutableAssessmentContract save(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody ExecutableAssessmentContract contract) {
        return service.save(courseId, quizId, questionId, contract);
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ExecutableAssessmentContract getForTeacher(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        return service.getForTeacher(courseId, quizId, questionId);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('TEACHER')")
    public CodeRunnerVerificationResponse verify(
            @PathVariable UUID courseId,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId) {
        return service.verify(courseId, quizId, questionId);
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    public List<TeacherCodeSubmissionView> submissions(
            @PathVariable UUID courseId, @PathVariable UUID quizId, @PathVariable UUID questionId) {
        return submissions.list(courseId, quizId, questionId);
    }

    @PostMapping("/submissions/{submissionId}/regrade")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TeacherCodeSubmissionView regrade(
            @PathVariable UUID courseId, @PathVariable UUID quizId, @PathVariable UUID questionId,
            @PathVariable UUID submissionId, @RequestBody(required = false) RegradeRequest request) {
        return submissions.regrade(courseId, quizId, questionId, submissionId,
                request == null ? null : request.reason());
    }

    public record RegradeRequest(String reason) { }
}
