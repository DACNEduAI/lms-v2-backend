package edu.ai.lms.core.quiz.app.service.crudquiz;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.ModifyQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.ModifyQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import edu.ai.lms.core.quiz.app.service.quizworkflow.QuizAccessPolicy;
import edu.ai.lms.common.event.SystemNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModifyQuizService implements ModifyQuizUseCase {

    private final SaveQuizPort saveQuizPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;
    private final ApplicationEventPublisher eventPublisher;
    private final QuestionVisualJobPort visualJobs;

    @Override
    @Transactional
    public ModifyQuizResponse modifyQuiz(ModifyQuizCommand command) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz existingQuiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        // Verify quiz belongs to the same course
        if (!existingQuiz.getCourseId().equals(command.courseId())) {
            throw new IllegalArgumentException("Quiz does not belong to this course");
        }

        // Verify user is the creator of the quiz
        verifyQuizOwnership(user, existingQuiz);
        QuizAccessPolicy.requireMutable(existingQuiz);

        Quiz requestedQuiz = command.quiz();
        Quiz updatedQuiz = mergeQuiz(existingQuiz, requestedQuiz, command.clearAttemptLimit());
        validateQuizTimeConfiguration(updatedQuiz);
        if (Boolean.TRUE.equals(updatedQuiz.getDisplay())
                && visualJobs.hasUnreadyRequiredVisual(updatedQuiz.getId())) {
            throw new IllegalArgumentException(
                    "Quiz cannot be published while a required visual is pending, processing, or failed");
        }

        Quiz savedQuiz = saveQuizPort.save(updatedQuiz);

        // Notify students if quiz is set to display
        if (user.getRole() == Roles.TEACHER && Boolean.TRUE.equals(savedQuiz.getDisplay())) {
            try {
                List<UUID> studentIdList = loadCourseMembershipPort.getStudentIdsByCourseId(command.courseId());
                if (!studentIdList.isEmpty()) {
                    Set<UUID> studentIds = new HashSet<>(studentIdList);
                    eventPublisher.publishEvent(new SystemNotificationEvent(
                            studentIds,
                            "Cập nhật bài kiểm tra!",
                            "Giảng viên vừa cập nhật bài kiểm tra: " + savedQuiz.getQuizTitle(),
                            "COURSE_UPDATE",
                            "/student/quizzes"));
                }
            } catch (Exception e) {
                // Log error but don't fail the whole operation
                e.printStackTrace();
            }
        }

        return ModifyQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .quizTitle(savedQuiz.getQuizTitle())
                .description(savedQuiz.getDescription())
                .unlimited(savedQuiz.getUnlimited())
                .timeLimitMinutes(savedQuiz.getTimeLimitMinutes())
                .passScore(savedQuiz.getPassScore())
                .startTime(savedQuiz.getStartTime())
                .dueTime(savedQuiz.getDueTime())
                .attemptLimited(savedQuiz.getAttemptLimited())
                .courseId(savedQuiz.getCourseId())
                .display(savedQuiz.getDisplay())
                .updatedAt(savedQuiz.getUpdatedAt())
                .build();
    }

    private Quiz mergeQuiz(Quiz existingQuiz, Quiz requestedQuiz, boolean clearAttemptLimit) {
        Quiz merged = Quiz.builder()
                .id(existingQuiz.getId())
                .quizTitle(requestedQuiz.getQuizTitle() != null ? requestedQuiz.getQuizTitle() : existingQuiz.getQuizTitle())
                .description(requestedQuiz.getDescription() != null ? requestedQuiz.getDescription() : existingQuiz.getDescription())
                .unlimited(requestedQuiz.getUnlimited() != null ? requestedQuiz.getUnlimited() : existingQuiz.getUnlimited())
                .timeLimitMinutes(requestedQuiz.getTimeLimitMinutes() != null ? requestedQuiz.getTimeLimitMinutes() : existingQuiz.getTimeLimitMinutes())
                .passScore(requestedQuiz.getPassScore() != null ? requestedQuiz.getPassScore() : existingQuiz.getPassScore())
                .startTime(requestedQuiz.getStartTime() != null ? requestedQuiz.getStartTime() : existingQuiz.getStartTime())
                .dueTime(requestedQuiz.getDueTime() != null ? requestedQuiz.getDueTime() : existingQuiz.getDueTime())
                .attemptLimited(clearAttemptLimit
                        ? null
                        : requestedQuiz.getAttemptLimited() != null
                                ? requestedQuiz.getAttemptLimited()
                                : existingQuiz.getAttemptLimited())
                .courseId(existingQuiz.getCourseId())
                .studentId(existingQuiz.getStudentId())
                .teacherId(existingQuiz.getTeacherId())
                .display(requestedQuiz.getDisplay() != null ? requestedQuiz.getDisplay() : existingQuiz.getDisplay())
                .lifecycleStatus(existingQuiz.getLifecycleStatus())
                .sourceType(existingQuiz.getSourceType())
                .originalQuizId(existingQuiz.getOriginalQuizId())
                .version(existingQuiz.getVersion())
                .publishedBy(existingQuiz.getPublishedBy())
                .publishedAt(existingQuiz.getPublishedAt())
                .createdAt(existingQuiz.getCreatedAt())
                .updatedAt(existingQuiz.getUpdatedAt())
                .build();

        return merged;
    }

    private void validateQuizTimeConfiguration(Quiz quiz) {
        if (quiz.getUnlimited() == null) {
            quiz.setUnlimited(false);
        }

        if (Boolean.TRUE.equals(quiz.getUnlimited())) {
            if (quiz.getStartTime() != null || quiz.getDueTime() != null) {
                throw new IllegalArgumentException("When unlimited is true, startTime and dueTime must be null");
            }
            quiz.setStartTime(null);
            quiz.setDueTime(null);
            return;
        }

        if (quiz.getDueTime() == null) {
            throw new IllegalArgumentException("dueTime is required when unlimited is false");
        }

        LocalDateTime startTime = quiz.getStartTime();
        LocalDateTime dueTime = quiz.getDueTime();
        if (startTime != null && dueTime.isBefore(startTime)) {
            throw new IllegalArgumentException("dueTime must be after or equal to startTime");
        }
    }

    private void verifyQuizOwnership(CurrentActor user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to modify this quiz");
        }
    }
}
