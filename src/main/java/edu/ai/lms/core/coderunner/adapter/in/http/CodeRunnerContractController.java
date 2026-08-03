package edu.ai.lms.core.coderunner.adapter.in.http;

import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.adapter.in.http.response.CodeRunnerVerificationResponse;
import edu.ai.lms.core.coderunner.app.service.CodeRunnerContractService;
import edu.ai.lms.core.coderunner.app.service.CodeRunnerVerificationService;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/code-runner")
public class CodeRunnerContractController {

    private final CodeRunnerContractService service;
    private final CodeRunnerVerificationService verificationService;

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
        ExecutableAssessmentContract contract = service.getForTeacher(courseId, quizId, questionId);
        return verificationService.verifyReference(contract);
    }
}
