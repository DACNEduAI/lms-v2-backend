package edu.ai.lms.core.coderunner.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.coderunner.adapter.in.http.response.CodeRunnerVerificationResponse;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerActor;
import edu.ai.lms.core.coderunner.app.domain.CodeRunnerQuestionContext;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.domain.StoredCodeRunnerContract;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerAccessDeniedException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerNotFoundException;
import edu.ai.lms.core.coderunner.app.port.out.CheckCodeRunnerCourseAccessPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerActorPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerQuestionContextPort;
import edu.ai.lms.core.coderunner.app.port.out.SaveCodeRunnerContractPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodeRunnerContractService {

    private static final Set<String> MUTABLE_QUIZ_STATUSES = Set.of("DRAFT", "REVIEW_PENDING");

    private final LoadCodeRunnerActorPort actorPort;
    private final LoadCodeRunnerQuestionContextPort questionContextPort;
    private final CheckCodeRunnerCourseAccessPort courseAccessPort;
    private final SaveCodeRunnerContractPort saveContractPort;
    private final LoadCodeRunnerContractPort loadContractPort;
    private final CodeRunnerVerificationService verificationService;

    public ExecutableAssessmentContract save(
            UUID courseId,
            UUID quizId,
            UUID questionId,
            ExecutableAssessmentContract contract) {
        CodeRunnerActor actor = requireTeacher();
        CodeRunnerQuestionContext context = requireContext(courseId, quizId, questionId);
        requireCourseAccess(actor, context);
        if (!MUTABLE_QUIZ_STATUSES.contains(context.quizLifecycleStatus())) {
            throw new CodeRunnerConflictException(
                    "Code Runner contract is locked because the quiz status is " + context.quizLifecycleStatus());
        }
        ExecutableAssessmentContract reviewContract = contract.withStatus("READY_FOR_REVIEW");
        reviewContract.validateForPersistence();
        saveContractPort.saveRevision(questionId, reviewContract);
        return loadContractPort.findLatestByQuestionId(questionId)
                .orElseThrow(() -> new IllegalStateException("Saved Code Runner contract could not be reloaded"));
    }

    public ExecutableAssessmentContract getForTeacher(UUID courseId, UUID quizId, UUID questionId) {
        return getStoredForTeacher(courseId, quizId, questionId).contract();
    }

    public StoredCodeRunnerContract getStoredForTeacher(UUID courseId, UUID quizId, UUID questionId) {
        CodeRunnerActor actor = requireTeacher();
        CodeRunnerQuestionContext context = requireContext(courseId, quizId, questionId);
        requireCourseAccess(actor, context);
        return loadContractPort.findLatestStoredByQuestionId(questionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException("Code Runner contract not found"));
    }

    public CodeRunnerVerificationResponse verify(UUID courseId, UUID quizId, UUID questionId) {
        CodeRunnerActor actor = requireTeacher();
        CodeRunnerQuestionContext context = requireContext(courseId, quizId, questionId);
        requireCourseAccess(actor, context);
        
        StoredCodeRunnerContract stored = loadContractPort.findLatestStoredByQuestionId(questionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException("Code Runner contract not found"));
                
        return verificationService.verifyReference(stored.configId(), questionId, actor.userId(), stored.contract());
    }

    private CodeRunnerActor requireTeacher() {
        CodeRunnerActor actor = actorPort.loadCurrentActor()
                .orElseThrow(() -> new CodeRunnerAccessDeniedException("User is not authenticated"));
        if (actor.role() != Roles.TEACHER) {
            throw new CodeRunnerAccessDeniedException("Only a course teacher can manage Code Runner contracts");
        }
        return actor;
    }

    private CodeRunnerQuestionContext requireContext(UUID courseId, UUID quizId, UUID questionId) {
        CodeRunnerQuestionContext context = questionContextPort.findQuestionContext(questionId)
                .orElseThrow(() -> new CodeRunnerNotFoundException("Question not found"));
        if (!courseId.equals(context.courseId()) || !quizId.equals(context.quizId())) {
            throw new CodeRunnerNotFoundException("Question not found in this course and quiz");
        }
        if (!"CODE_RUNNER".equals(context.questionType())) {
            throw new CodeRunnerConflictException("Question type is not CODE_RUNNER");
        }
        return context;
    }

    private void requireCourseAccess(CodeRunnerActor actor, CodeRunnerQuestionContext context) {
        if (context.teacherId() == null
                || !courseAccessPort.isTeacherInCourse(actor.userId(), context.courseId())) {
            throw new CodeRunnerAccessDeniedException("Teacher is not assigned to this course");
        }
    }
}
