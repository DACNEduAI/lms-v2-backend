package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeRunnerQuestion;
import edu.ai.lms.core.attempt.app.domain.AttemptCodeSubmission;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import edu.ai.lms.core.attempt.app.exception.AttemptNotFoundException;
import edu.ai.lms.core.attempt.app.exception.InvalidAttemptSubmissionException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.out.AttemptCodeRunnerPort;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttemptCodeRunnerService {
    private static final long GRACE_SECONDS = 5;

    private final LoadSignedInUserPort actorPort;
    private final LoadAttemptPort attemptPort;
    private final LoadQuizAttemptPolicyPort quizPolicyPort;
    private final AttemptCodeRunnerPort codeRunnerPort;

    public AttemptCodeRunnerQuestion getQuestion(UUID courseId, UUID attemptId, UUID questionId) {
        Context context = requireActiveAttempt(courseId, attemptId);
        return codeRunnerPort.getQuestion(attemptId, context.attempt().getQuizId(), questionId);
    }

    public AttemptCodeSubmission getSubmission(UUID courseId, UUID attemptId, UUID questionId, UUID submissionId) {
        Context context = requireAttemptAccess(courseId, attemptId);
        boolean isFinished = context.attempt().getStatus() == Attempt.AttemptStatus.FINISHED;
        return codeRunnerPort.getSubmission(context.actor().getId(), submissionId, !isFinished);
    }

    public UUID run(UUID courseId, UUID attemptId, UUID questionId,
                                     String sourceCode, String idempotencyKey, String mode) {
        if (!"RUN".equals(mode) && !"SUBMIT".equals(mode)) {
            throw new IllegalArgumentException("mode must be RUN or SUBMIT");
        }
        Context context = requireActiveAttempt(courseId, attemptId);
        return codeRunnerPort.execute(attemptId, context.actor().getId(), context.attempt().getQuizId(),
                questionId, sourceCode, idempotencyKey, mode);
    }

    private Context requireActiveAttempt(UUID courseId, UUID attemptId) {
        CurrentActor actor = actorPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));
        if (actor.getRole() != Roles.STUDENT) {
            throw new InvalidAttemptSubmissionException("Only students can execute attempt code");
        }
        if (!quizPolicyPort.isStudentInCourse(actor.getId(), courseId)) {
            throw new InvalidAttemptSubmissionException("Student is not enrolled in this course");
        }
        Attempt attempt = attemptPort.getByIdAndStudentId(attemptId, actor.getId())
                .orElseThrow(() -> new AttemptNotFoundException("Active attempt not found"));
        if (attempt.getStatus() != Attempt.AttemptStatus.STARTED) {
            throw new InvalidAttemptSubmissionException("Attempt is no longer active");
        }
        if (attempt.getDueTime() != null
                && LocalDateTime.now().isAfter(attempt.getDueTime().plusSeconds(GRACE_SECONDS))) {
            throw new InvalidAttemptSubmissionException("Attempt due time has passed");
        }
        quizPolicyPort.getQuizAttemptPolicy(courseId, attempt.getQuizId())
                .orElseThrow(() -> new InvalidAttemptSubmissionException("Attempt quiz is not in this course"));
        return new Context(actor, attempt);
    }

    private Context requireAttemptAccess(UUID courseId, UUID attemptId) {
        CurrentActor actor = actorPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));
        if (actor.getRole() != Roles.STUDENT) {
            throw new InvalidAttemptSubmissionException("Only students can execute attempt code");
        }
        if (!quizPolicyPort.isStudentInCourse(actor.getId(), courseId)) {
            throw new InvalidAttemptSubmissionException("Student is not enrolled in this course");
        }
        Attempt attempt = attemptPort.getByIdAndStudentId(attemptId, actor.getId())
                .orElseThrow(() -> new AttemptNotFoundException("Attempt not found"));
        quizPolicyPort.getQuizAttemptPolicy(courseId, attempt.getQuizId())
                .orElseThrow(() -> new InvalidAttemptSubmissionException("Attempt quiz is not in this course"));
        return new Context(actor, attempt);
    }

    private record Context(CurrentActor actor, Attempt attempt) { }
}
