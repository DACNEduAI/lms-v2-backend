package edu.ai.lms.core.quiz.app.service.crudquiz;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetQuizResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuestionLearningOutcomeView;
import edu.ai.lms.core.quiz.app.domain.QuizLearningOutcomeView;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotVisible;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.GetQuizUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;
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
    private final LoadQuestionLearningOutcomePort loadQuestionLearningOutcomePort;

    @Override
    @Transactional(readOnly = true)
    public GetQuizResponse getQuiz(UUID courseId, UUID quizId) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
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

        List<QuestionLearningOutcomeView> allQuestionOutcomes = loadQuestionLearningOutcomePort
                .findLearningOutcomesByQuizId(quizId);
        Map<UUID, List<QuestionLearningOutcomeView>> outcomesByQuestion = allQuestionOutcomes.stream()
                .collect(Collectors.groupingBy(QuestionLearningOutcomeView::getQuestionId));

        List<GetQuizResponse.QuestionSummary> questionSummaries = quiz.getQuestions().stream()
                .map(q -> {
                    List<GetQuizResponse.LearningOutcomeSummary> loSummaries = outcomesByQuestion.getOrDefault(q.getId(), List.of())
                            .stream()
                            .map(lo -> GetQuizResponse.LearningOutcomeSummary.builder()
                                    .id(lo.getLearningOutcomeId())
                                    .code(lo.getLearningOutcomeCode())
                                    .prefixNumber(lo.getLearningOutcomePrefixNumber())
                                    .description(lo.getLearningOutcomeDescription())
                                    .build())
                            .toList();

                    return GetQuizResponse.QuestionSummary.builder()
                            .questionId(q.getId())
                            .content(q.getContent())
                            .type(q.getType() != null ? q.getType().name() : null)
                            .learningOutcomes(loSummaries)
                            .answers(q.getAnswers().stream()
                                    .map(a -> GetQuizResponse.AnswerSummary.builder()
                                            .answerNumber(a.getAnswerNumber())
                                            .content(a.getContent())
                                            .correct(a.isCorrect())
                                            .explanation(a.getExplanation())
                                            .build())
                                    .collect(Collectors.toList()))
                            .images((q.getImages() == null ? List.<edu.ai.lms.core.quiz.app.domain.Question.Image>of() : q.getImages())
                                    .stream().map(image -> GetQuizResponse.QuestionImageSummary.builder()
                                            .id(image.getId())
                                            .sourceType(image.getSourceType() == null ? null : image.getSourceType().name())
                                            .sourceText(image.getSourceText())
                                            .generationBrief(image.getGenerationBrief())
                                            .altText(image.getAltText())
                                            .status(image.getStatus() == null ? null : image.getStatus().name())
                                            .imageName(image.getImageName())
                                            .url(image.getUrl())
                                            .mediaType(image.getMediaType())
                                            .errorCode(image.getErrorCode())
                                            .errorMessage(image.getErrorMessage())
                                            .build()).toList())
                            .build();
                })
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
                .description(quiz.getDescription())
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
                .prefixNumber(learningOutcome.getLearningOutcomePrefixNumber())
                .description(learningOutcome.getLearningOutcomeDescription())
                .build();
    }

    private void verifyTeacherInCourse(CurrentActor user, UUID courseId) {
        if (!loadCourseMembershipPort.isTeacherInCourse(user.getId(), courseId)) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }

    private void verifyStudentInCourse(CurrentActor user, UUID courseId) {
        if (!loadCourseMembershipPort.isStudentInCourse(user.getId(), courseId)) {
            throw new IllegalArgumentException("User is not a member of this course");
        }
    }
}
