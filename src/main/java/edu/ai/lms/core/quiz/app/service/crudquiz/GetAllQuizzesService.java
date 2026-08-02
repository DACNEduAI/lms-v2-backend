package edu.ai.lms.core.quiz.app.service.crudquiz;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetAllQuizzesCreatedByStudentUseCase;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetAllQuizzesCreatedByTeacherUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllQuizzesService implements GetAllQuizzesCreatedByTeacherUseCase, GetAllQuizzesCreatedByStudentUseCase {

    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;

    @Override
    @Transactional(readOnly = true)
    public GetAllQuizzesResponse getAllQuizzesCreatedByTeacher(UUID courseId, int pageSize, String lastCreated) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        verifyCourseMembershipForTeacherCreatedListing(user, courseId);

        List<Quiz> quizzes = loadQuizPort.getTeacherCreatedByCourseId(courseId);
        if (user.getRole() == Roles.STUDENT) {
            quizzes = quizzes.stream()
                .filter(quiz -> Boolean.TRUE.equals(quiz.getDisplay()))
                .collect(Collectors.toList());
        }

        return buildResponse(quizzes, pageSize, lastCreated);
    }

    @Override
    @Transactional(readOnly = true)
    public GetAllQuizzesResponse getAllQuizzesCreatedByStudent(UUID courseId, int pageSize, String lastCreated) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        if (user.getRole() != Roles.STUDENT) {
            throw new IllegalArgumentException("Only students can view student-created quizzes");
        }

        verifyStudentMembership(user.getId(), courseId);

        List<Quiz> quizzes = loadQuizPort.getStudentCreatedByCourseIdAndStudentId(courseId, user.getId());
        return buildResponse(quizzes, pageSize, lastCreated);
    }

    private GetAllQuizzesResponse buildResponse(List<Quiz> quizzes, int pageSize, String lastCreated) {
        Comparator<Quiz> comparator = Comparator.comparing(
                Quiz::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));

        if ("des".equalsIgnoreCase(lastCreated) || "desc".equalsIgnoreCase(lastCreated)) {
            comparator = comparator.reversed();
        }

        String appliedLastCreated =
                ("des".equalsIgnoreCase(lastCreated) || "desc".equalsIgnoreCase(lastCreated))
                        ? "des"
                        : "asc";

        List<GetAllQuizzesResponse.QuizSummary> quizSummaries = quizzes.stream()
                .sorted(comparator)
                .limit(pageSize)
                .map(quiz -> GetAllQuizzesResponse.QuizSummary.builder()
                        .quizId(quiz.getId())
                        .quizTitle(quiz.getQuizTitle())
                    .unlimited(quiz.getUnlimited())
                        .timeLimitMinutes(quiz.getTimeLimitMinutes())
                        .passScore(quiz.getPassScore())
                        .startTime(quiz.getStartTime())
                        .dueTime(quiz.getDueTime())
                        .attemptLimited(quiz.getAttemptLimited())
                        .display(quiz.getDisplay())
                        .lifecycleStatus(quiz.getLifecycleStatus())
                        .sourceType(quiz.getSourceType())
                        .originalQuizId(quiz.getOriginalQuizId())
                        .version(quiz.getVersion())
                        .publishedBy(quiz.getPublishedBy())
                        .publishedAt(quiz.getPublishedAt())
                        .createdAt(quiz.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return GetAllQuizzesResponse.builder()
                .quizzes(quizSummaries)
                .totalQuizzes(quizzes.size())
                .pageSize(pageSize)
                .lastCreated(appliedLastCreated)
                .build();
    }

    private void verifyTeacherMembership(UUID teacherId, UUID courseId) {
        boolean isMember = loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId);
        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }

    private void verifyStudentMembership(UUID studentId, UUID courseId) {
        boolean isMember = loadCourseMembershipPort.isStudentInCourse(studentId, courseId);
        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }

    private void verifyCourseMembershipForTeacherCreatedListing(CurrentActor user, UUID courseId) {
        if (user.getRole() == Roles.TEACHER) {
            verifyTeacherMembership(user.getId(), courseId);
            return;
        }

        if (user.getRole() == Roles.STUDENT) {
            verifyStudentMembership(user.getId(), courseId);
            return;
        }

        throw new IllegalArgumentException("Only teachers and students can view teacher-created quizzes");
    }
}
