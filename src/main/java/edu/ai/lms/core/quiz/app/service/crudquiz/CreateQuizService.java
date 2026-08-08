package edu.ai.lms.core.quiz.app.service.crudquiz;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.common.event.SystemActivityEvent;
import edu.ai.lms.common.event.SystemNotificationEvent;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.CreateQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.InvalidLearningOutcomeException;
import edu.ai.lms.core.quiz.app.exception.MissingLearningOutcomeException;
import edu.ai.lms.core.quiz.app.exception.TimeValidationException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthorizedException;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.CreateQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;
import lombok.RequiredArgsConstructor;

/**
 * Creates quizzes for teachers and students in a course context.
 *
 * <p>
 * Business responsibilities:
 * <ul>
 * <li>Requires an authenticated user and validates course membership before
 * write operations.</li>
 * <li>Applies role-based ownership: teacher-created quizzes store
 * {@code teacherId}, student-created quizzes store {@code studentId}.</li>
 * <li>Enforces time policy integrity for teacher-created quizzes.</li>
 * <li>Normalizes student-created quizzes to unlimited mode and clears
 * teacher-only scheduling controls.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CreateQuizService implements CreateQuizUseCase {

    private final SaveQuizPort saveQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;
    private final LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;
    private final SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a quiz in a course after authentication, membership checks, and
     * business validation.
     *
     * @param command input command containing {@code courseId} and quiz payload
     * @return persisted quiz metadata for API response
     */
    @Override
    @Transactional
    public CreateQuizResponse createQuiz(CreateQuizCommand command) {
        // Verify user is authenticated
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        // Verify user belongs to the course
        verifyCourseMembership(user, command.courseId());

        // Set quiz properties
        Quiz quiz = command.quiz();
        quiz.setCourseId(command.courseId());
        if (quiz.getUnlimited() == null) {
            quiz.setUnlimited(false);
        }
        if (quiz.getDisplay() == null) {
            quiz.setDisplay(true);
        }

        if (user.getRole() == Roles.TEACHER) {
            quiz.setTeacherId(user.getId());
            validateQuizTimeConfiguration(quiz);

        } else {
            quiz.setStudentId(user.getId());
            quiz.setUnlimited(true);
            quiz.setStartTime(null);
            quiz.setDueTime(null);
            quiz.setTimeLimitMinutes(null);
            quiz.setAttemptLimited(null);
        }

        List<UUID> learningOutcomeIds = resolveLearningOutcomeIds(command.courseId(), command.learningOutcomeIds());

        // Save quiz
        Quiz savedQuiz = saveQuizPort.save(quiz);

        saveQuizLearningOutcomePort.addLearningOutcomes(savedQuiz.getId(), learningOutcomeIds);

        // Publish activity event (lịch sử hoạt động của giảng viên)
        eventPublisher.publishEvent(SystemActivityEvent.builder()
                .userId(user.getId())
                .actionType("CREATE")
                .entityType("QUIZ")
                .entityId(savedQuiz.getId())
                .entityName(savedQuiz.getQuizTitle())
                .description(user.getRole() == Roles.TEACHER ? "Giảng viên tạo bài kiểm tra mới"
                        : "Học viên tạo bài kiểm tra mới")
                .timestamp(LocalDateTime.now())
                .build());

        // Publish notification event — chỉ khi giảng viên tạo quiz hiển thị công khai
        if (user.getRole() == Roles.TEACHER && Boolean.TRUE.equals(savedQuiz.getDisplay())) {
            try {
                List<UUID> studentIdList = loadCourseMembershipPort.getStudentIdsByCourseId(command.courseId());
                if (!studentIdList.isEmpty()) {
                    Set<UUID> studentIds = new HashSet<>(studentIdList);
                    eventPublisher.publishEvent(new SystemNotificationEvent(
                            studentIds,
                            "Có bài kiểm tra mới!",
                            "Giảng viên vừa tạo bài kiểm tra: " + savedQuiz.getQuizTitle(),
                            "COURSE_UPDATE",
                            "/student/quizzes"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return CreateQuizResponse.builder()
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
                .createdAt(savedQuiz.getCreatedAt())
                .build();
    }

    /**
     * Validates quiz timing consistency.
     *
     * <p>
     * Rules:
     * <ul>
     * <li>If unlimited is true: startTime, dueTime, and timeLimitMinutes must all
     * be null.</li>
     * <li>If unlimited is false: dueTime is required.</li>
     * <li>If startTime is present: dueTime must be after or equal to
     * startTime.</li>
     * </ul>
     *
     * @param quiz quiz draft to validate and normalize
     */
    private void validateQuizTimeConfiguration(Quiz quiz) {
        if (Boolean.TRUE.equals(quiz.getUnlimited())) {
            if (quiz.getStartTime() != null || quiz.getDueTime() != null) {
                throw new TimeValidationException("When unlimited is true, startTime and dueTime must be null");
            }
            if (quiz.getTimeLimitMinutes() != null) {
                throw new TimeValidationException("When unlimited is true, timeLimitMinutes must be null");
            }
            quiz.setStartTime(null);
            quiz.setDueTime(null);
            quiz.setTimeLimitMinutes(null);
            return;
        }

        if (quiz.getDueTime() == null) {
            throw new TimeValidationException("dueTime is required when unlimited is false");
        }

        LocalDateTime startTime = quiz.getStartTime();
        LocalDateTime dueTime = quiz.getDueTime();
        if (startTime != null && dueTime.isBefore(startTime)) {
            throw new TimeValidationException("dueTime must be after or equal to startTime");
        }
    }

    private List<UUID> resolveLearningOutcomeIds(UUID courseId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds == null || learningOutcomeIds.isEmpty()) {
            throw new MissingLearningOutcomeException("Quiz must include at least one learning outcome");
        }

        Set<UUID> uniqueIds = new HashSet<>(learningOutcomeIds);
        List<UUID> existingIds = loadCourseLearningOutcomePort
                .findExistingLearningOutcomeIdsByCourseIdAndIds(courseId, List.copyOf(uniqueIds));
        if (existingIds.size() != uniqueIds.size()) {
            throw new InvalidLearningOutcomeException("Some learning outcomes do not belong to this course");
        }

        return new ArrayList<>(uniqueIds);
    }

    /**
     * Verifies that the authenticated user belongs to the target course.
     *
     * @param user     authenticated user identity
     * @param courseId target course
     */
    private void verifyCourseMembership(CurrentActor user, java.util.UUID courseId) {
        boolean isMember = user.getRole() == Roles.TEACHER
                ? loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)
                : loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId);

        if (!isMember) {
            throw new UserNotAuthorizedException("User is not a member of this course");
        }
    }
}
