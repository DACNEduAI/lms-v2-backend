package edu.ai.lms.core.quiz.app.service.crudquiz;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuizLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuizLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.AddQuizLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizLearningOutcomePort;
import lombok.RequiredArgsConstructor;
import edu.ai.lms.core.quiz.app.service.quizworkflow.QuizAccessPolicy;

@Service
@RequiredArgsConstructor
public class AddQuizLearningOutcomesService implements AddQuizLearningOutcomesUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;
    private final SaveQuizLearningOutcomePort saveQuizLearningOutcomePort;
    private final LoadQuizLearningOutcomePort loadQuizLearningOutcomePort;

    @Override
    @Transactional
    public QuizLearningOutcomeOperationResponse addLearningOutcomes(AddQuizLearningOutcomesCommand command) {
        CurrentActor user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        validateQuizOwnershipAndCourse(user, quiz, command.courseId(), "modify");
        QuizAccessPolicy.requireMutable(quiz);

        UUID quizCourseId = quiz.getCourseId();

        List<UUID> resolvedLearningOutcomeIds = resolveLearningOutcomeIds(
            quizCourseId,
                command.learningOutcomeIds(),
                command.groupLearningOutcomeIds());

        saveQuizLearningOutcomePort.addLearningOutcomes(quiz.getId(), resolvedLearningOutcomeIds);

        List<UUID> finalLearningOutcomeIds = loadQuizLearningOutcomePort.findLearningOutcomeIdsByQuizId(quiz.getId());

        return QuizLearningOutcomeOperationResponse.builder()
                .quizId(quiz.getId())
            .courseId(quizCourseId)
                .operation("ADD")
                .totalAffected(resolvedLearningOutcomeIds.size())
                .learningOutcomeIds(finalLearningOutcomeIds)
                .build();
    }

    private void validateQuizOwnershipAndCourse(CurrentActor user, Quiz quiz, UUID courseId, String action) {
        if (!quiz.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Quiz does not belong to this course");
        }

        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to " + action + " this quiz");
        }
    }

    private List<UUID> resolveLearningOutcomeIds(UUID courseId, List<UUID> learningOutcomeIds, List<UUID> groupLearningOutcomeIds) {
        Set<UUID> directIds = toUniqueSet(learningOutcomeIds);
        Set<UUID> groupIds = toUniqueSet(groupLearningOutcomeIds);

        if (directIds.isEmpty() && groupIds.isEmpty()) {
            throw new IllegalArgumentException("At least one learning outcome or group learning outcome id is required");
        }

        if (!groupIds.isEmpty()) {
            List<UUID> existingGroupIds = loadCourseLearningOutcomePort
                    .findExistingGroupLearningOutcomeIdsByCourseIdAndIds(courseId, new ArrayList<>(groupIds));
            if (existingGroupIds.size() != groupIds.size()) {
                throw new IllegalArgumentException("Some group learning outcomes do not belong to this course");
            }

            List<UUID> idsFromGroups = loadCourseLearningOutcomePort
                    .findLearningOutcomeIdsByCourseIdAndGroupIds(courseId, new ArrayList<>(groupIds));
            directIds.addAll(idsFromGroups);
        }

        List<UUID> existingLearningOutcomeIds = loadCourseLearningOutcomePort
                .findExistingLearningOutcomeIdsByCourseIdAndIds(courseId, new ArrayList<>(directIds));

        if (existingLearningOutcomeIds.size() != directIds.size()) {
            throw new IllegalArgumentException("Some learning outcomes do not belong to this course");
        }

        return new ArrayList<>(directIds);
    }

    private Set<UUID> toUniqueSet(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(ids);
    }
}
