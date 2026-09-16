package edu.ai.lms.core.attempt.app.service;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.attempt.adapter.in.response.GetMyAttemptsResponse;
import edu.ai.lms.core.attempt.app.domain.Attempt;
import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import edu.ai.lms.core.attempt.app.exception.QuizNotFoundForAttemptException;
import edu.ai.lms.core.attempt.app.exception.AttemptAccessDeniedException;
import edu.ai.lms.core.attempt.app.exception.UserNotAuthenticatedForAttemptException;
import edu.ai.lms.core.attempt.app.port.in.usecase.GetMyAttemptsUseCase;
import edu.ai.lms.core.attempt.app.port.out.LoadAttemptPort;
import edu.ai.lms.core.attempt.app.port.out.LoadQuizAttemptPolicyPort;
import edu.ai.lms.core.attempt.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
import edu.ai.lms.core.attempt.app.domain.CurrentActor;
import edu.ai.lms.core.attempt.app.domain.QuizAttemptPolicy;
import edu.ai.lms.core.course.api.LearningOutcomeCatalogApi;
import edu.ai.lms.core.course.api.LearningOutcomeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyAttemptsService implements GetMyAttemptsUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizAttemptPolicyPort loadQuizAttemptPolicyPort;
    private final LoadAttemptPort loadAttemptPort;
    private final AttemptAutoCloseService attemptAutoCloseService;
    private final AttemptQuestionResultPort attemptQuestionResultPort;
    private final LearningOutcomeCatalogApi learningOutcomeCatalogApi;

    @Override
    @Transactional
    public GetMyAttemptsResponse getMyAttempts(UUID courseId, UUID quizId) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedForAttemptException("User not authenticated"));

        if (user.getRole() != Roles.STUDENT) {
            throw new AttemptAccessDeniedException("Only students can view attempts");
        }

        if (!loadQuizAttemptPolicyPort.isStudentInCourse(user.getId(), courseId)) {
            throw new AttemptAccessDeniedException("Student is not enrolled in this course");
        }

        QuizAttemptPolicy policy = loadQuizAttemptPolicyPort.getQuizAttemptPolicy(courseId, quizId)
                .orElseThrow(() -> new QuizNotFoundForAttemptException("Quiz not found in course"));

        attemptAutoCloseService.isAlreadyStartedAttemptExpiredValidation(user.getId(), courseId, quizId, LocalDateTime.now());

        List<Attempt> attemptDomains = loadAttemptPort.getByStudentAndQuiz(user.getId(), quizId);
        List<AttemptQuestionResult> records = attemptQuestionResultPort.findAttemptRecords(
                user.getId(), quizId, attemptDomains.stream().map(Attempt::getId).toList());
        Map<UUID, String> learningOutcomeCodes = learningOutcomeCatalogApi.findByIds(policy.getCourseId(), records.stream()
                        .map(AttemptQuestionResult::getLearningOutcomeId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(LearningOutcomeView::id, LearningOutcomeView::code));
        Map<UUID, List<AttemptQuestionResult>> recordsByAttempt = records
                .stream()
                .collect(Collectors.groupingBy(AttemptQuestionResult::getAttemptId));
        List<GetMyAttemptsResponse.AttemptItem> attempts = attemptDomains.stream()
                .map(attempt -> toItem(attempt, recordsByAttempt.getOrDefault(attempt.getId(), List.of()), learningOutcomeCodes))
                .toList();

        return GetMyAttemptsResponse.builder()
                .quizId(quizId)
                .attempts(attempts)
                .build();
    }

    private GetMyAttemptsResponse.AttemptItem toItem(
            Attempt attempt, List<AttemptQuestionResult> records, Map<UUID, String> learningOutcomeCodes) {
        return GetMyAttemptsResponse.AttemptItem.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .startTime(attempt.getStartTime())
                .dueTime(attempt.getDueTime())
                .submittedTime(attempt.getSubmittedTime())
                .timeSpentSeconds(attempt.getTimeSpentSeconds())
                .errorReason(attempt.getErrorReason())
                .attemptRecords(records.stream().map(record -> toRecordItem(record, learningOutcomeCodes)).toList())
                .build();
    }

    private GetMyAttemptsResponse.AttemptRecordItem toRecordItem(
            AttemptQuestionResult record, Map<UUID, String> learningOutcomeCodes) {
        return GetMyAttemptsResponse.AttemptRecordItem.builder()
                .questionId(record.getQuestionId())
                .interactionOrder(record.getInteractionOrder())
                .questionType(record.getQuestionType())
                .questionContent(record.getQuestionContent())
                .questionContentFormat(record.getQuestionContentFormat())
                .learningOutcomeId(record.getLearningOutcomeId())
                .learningOutcomeCode(learningOutcomeCodes.get(record.getLearningOutcomeId()))
                .learningOutcomeMappingCount(record.getLearningOutcomeMappingCount())
                .studentAnswer(toAnswer(record.getStudentAnswerNumber(), record.getStudentAnswerPosition(),
                        record.getStudentAnswerContent(), record.getStudentAnswerContentFormat()))
                .correctAnswer(toAnswer(record.getCorrectAnswerNumber(), record.getCorrectAnswerPosition(),
                        record.getCorrectAnswerContent(), record.getCorrectAnswerContentFormat()))
                .isCorrect(Boolean.TRUE.equals(record.getCorrect()))
                .snapshotComplete(Boolean.TRUE.equals(record.getSnapshotComplete()))
                .build();
    }

    private GetMyAttemptsResponse.AnswerItem toAnswer(
            Long number, Integer position, String content, String format) {
        if (number == null) return null;
        return GetMyAttemptsResponse.AnswerItem.builder()
                .answerNumber(number)
                .displayedPosition(position)
                .displayedLabel(position == null ? null : answerLabel(position))
                .content(content)
                .contentFormat(format)
                .build();
    }

    private String answerLabel(int zeroBasedPosition) {
        StringBuilder label = new StringBuilder();
        int value = zeroBasedPosition + 1;
        while (value > 0) {
            value--;
            label.append((char) ('A' + value % 26));
            value /= 26;
        }
        return label.reverse().toString();
    }
}
