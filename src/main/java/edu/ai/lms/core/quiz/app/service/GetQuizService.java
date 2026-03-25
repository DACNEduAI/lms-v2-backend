package edu.ai.lms.core.quiz.app.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLearningOutcomeView;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotVisible;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
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
    private final LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;

    @Override
    @Transactional(readOnly = true)
    public GetQuizResponse getQuiz(UUID courseId, UUID quizId) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        if (user.getRole() == Roles.TEACHER) {
            verifyTeacherInCourse(user, courseId);
        } else if (user.getRole() == Roles.STUDENT) {
            verifyStudentInCourse(user, courseId);
        } else {
            throw new IllegalArgumentException("Only teachers or students can access this quiz");
        }

        Quiz quiz = loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + quizId));

        if (user.getRole() == Roles.STUDENT && !Boolean.TRUE.equals(quiz.getDisplay())) {
            throw new QuizNotVisible("Quiz is not visible for student to view: " + quiz.getId());
        }

        List<GetQuizResponse.QuestionSummary> questionSummaries = quiz.getQuestions().stream()
                .map(q -> GetQuizResponse.QuestionSummary.builder()
                        .questionId(q.getId())
                        .content(q.getContent())
                        .type(q.getType() != null ? q.getType().name() : null)
                        .points(q.getPoints())
                        .answers(q.getAnswers().stream()
                                .map(a -> GetQuizResponse.AnswerSummary.builder()
                                        .answerNumber(a.getAnswerNumber())
                                        .content(a.getContent())
                                        .isCorrect(a.isCorrect())
                                        .explanation(a.getExplanation())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        List<QuizLearningOutcomeView> quizLearningOutcomes = loadCourseLearningOutcomePort
                .findQuizLearningOutcomes(courseId, quizId);

        List<GetQuizResponse.LearningOutcomeSummary> ungroupedLearningOutcomes = quizLearningOutcomes.stream()
                .filter(lo -> lo.getGroupLearningOutcomeId() == null)
                .map(this::toLearningOutcomeSummary)
                .toList();

        Map<UUID, List<QuizLearningOutcomeView>> groupedLearningOutcomesMap = quizLearningOutcomes.stream()
                .filter(lo -> lo.getGroupLearningOutcomeId() != null)
                .collect(Collectors.groupingBy(QuizLearningOutcomeView::getGroupLearningOutcomeId));

        List<GetQuizResponse.GroupLearningOutcomeSummary> groupedLearningOutcomes = groupedLearningOutcomesMap
                .values().stream()
                .map(groupRows -> {
                    QuizLearningOutcomeView groupHeader = groupRows.getFirst();
                    return GetQuizResponse.GroupLearningOutcomeSummary.builder()
                            .id(groupHeader.getGroupLearningOutcomeId())
                            .name(groupHeader.getGroupLearningOutcomeName())
                            .description(groupHeader.getGroupLearningOutcomeDescription())
                            .learningOutcomes(groupRows.stream()
                                    .map(this::toLearningOutcomeSummary)
                                    .toList())
                            .build();
                })
                .toList();

        return GetQuizResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getQuizTitle())
                .unlimited(quiz.getUnlimited())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .startTime(quiz.getStartTime())
                .dueTime(quiz.getDueTime())
                .attemptLimited(quiz.getAttemptLimited())
                .courseId(quiz.getCourseId())
                .display(quiz.getDisplay())
                .ungroupedLearningOutcomes(ungroupedLearningOutcomes)
                .groupedLearningOutcomes(groupedLearningOutcomes)
                .questions(questionSummaries)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }

    private GetQuizResponse.LearningOutcomeSummary toLearningOutcomeSummary(
            QuizLearningOutcomeView learningOutcome) {
        return GetQuizResponse.LearningOutcomeSummary.builder()
                .id(learningOutcome.getLearningOutcomeId())
                .code(learningOutcome.getLearningOutcomeCode())
                .description(learningOutcome.getLearningOutcomeDescription())
                .build();
    }

    private void verifyTeacherInCourse(UserAuth user, UUID courseId) {
        if (!loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }

    private void verifyStudentInCourse(UserAuth user, UUID courseId) {
        if (!loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId)) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }
}
