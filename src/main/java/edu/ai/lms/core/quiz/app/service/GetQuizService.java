package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetQuizService implements GetQuizUseCase {

    private final LoadQuizPort loadQuizPort;
    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadCourseMembershipPort loadCourseMembershipPort;

    @Override
    @Transactional(readOnly = true)
    public GetQuizResponse getQuiz(UUID courseId, UUID quizId) {
        User user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        // Verify user belongs to the course
        verifyCourseMembership(user, courseId);

        Quiz quiz = loadQuizPort.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + quizId));

        // Verify quiz belongs to the course
        if (!quiz.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Quiz does not belong to this course");
        }

        // Teachers can only see their own quizzes
        if (user.getRole() == Roles.TEACHER && !user.getId().equals(quiz.getTeacherId())) {
            throw new IllegalArgumentException("Teacher is not authorized to access this quiz");
        }

        // Build question summaries (Quiz loads questions but NOT answers)
        List<GetQuizResponse.QuestionSummary> questionSummaries = quiz.getQuestions().stream()
                .map(q -> GetQuizResponse.QuestionSummary.builder()
                        .questionId(q.getId())
                        .content(q.getContent())
                        .type(q.getType() != null ? q.getType().name() : null)
                        .points(q.getPoints())
                        .build())
                .collect(Collectors.toList());

        return GetQuizResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getQuizTitle())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .courseId(quiz.getCourseId())
                .active(quiz.getActive())
                .questions(questionSummaries)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }

    private void verifyCourseMembership(User user, UUID courseId) {
        boolean isMember = user.getRole() == Roles.TEACHER
                ? loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)
                : loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId);

        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }
}
