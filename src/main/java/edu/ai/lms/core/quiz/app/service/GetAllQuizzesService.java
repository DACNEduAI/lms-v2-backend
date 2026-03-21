package edu.ai.lms.core.quiz.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetAllQuizzesUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllQuizzesService implements GetAllQuizzesUseCase {

    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;

    // Strategy Pattern - Inner interface for loading quizzes
    private interface QuizLoadStrategy {
        List<Quiz> loadQuizzes(UUID userId, UUID courseId);
    }

    // Student strategy: see displayed quizzes created by teachers in the course.
    private class StudentQuizLoadStrategy implements QuizLoadStrategy {
        @Override
        public List<Quiz> loadQuizzes(UUID userId, UUID courseId) {
            return loadQuizPort.getDisplayedTeacherCreatedByCourseId(courseId);
        }
    }

    // Teacher strategy: see all quizzes created by teachers in the course.
    private class TeacherQuizLoadStrategy implements QuizLoadStrategy {
        @Override
        public List<Quiz> loadQuizzes(UUID userId, UUID courseId) {
            return loadQuizPort.getTeacherCreatedByCourseId(courseId);
        }
    }

    private QuizLoadStrategy selectStrategy(Roles role) {
        return switch (role) {
            case TEACHER -> new TeacherQuizLoadStrategy();
            case STUDENT -> new StudentQuizLoadStrategy();
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public GetAllQuizzesResponse getAllQuizzes(UUID courseId, int pageSize, String lastCreated) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        // Verify user belongs to the course
        verifyCourseMembership(user, courseId);

        // Select the appropriate strategy based on user's role
        QuizLoadStrategy strategy = selectStrategy(user.getRole());

        // Load quizzes using the selected strategy
        List<Quiz> quizzes = strategy.loadQuizzes(user.getId(), courseId);

        // Sort quizzes by title
        Comparator<Quiz> comparator = Comparator.comparing(
                Quiz::getStartTime,
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
                        .timeLimitMinutes(quiz.getTimeLimitMinutes())
                        .passScore(quiz.getPassScore())
                        .startTime(quiz.getStartTime())
                        .dueTime(quiz.getDueTime())
                        .attemptLimited(quiz.getAttemptLimited())
                        .display(quiz.getDisplay())
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

    private void verifyCourseMembership(UserAuth user, UUID courseId) {
        boolean isMember = user.getRole() == Roles.TEACHER
                ? loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)
                : loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId);

        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }
}
