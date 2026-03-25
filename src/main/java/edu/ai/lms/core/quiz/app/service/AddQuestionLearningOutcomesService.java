package edu.ai.lms.core.quiz.app.service;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.QuestionLearningOutcomeOperationResponse;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.command.AddQuestionLearningOutcomesCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.AddQuestionLearningOutcomesUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddQuestionLearningOutcomesService implements AddQuestionLearningOutcomesUseCase {

    private final LoadSignedInUserPort loadSignedInUserPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadCourseLearningOutcomePort loadCourseLearningOutcomePort;
    private final SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;
    private final LoadQuestionLearningOutcomePort loadQuestionLearningOutcomePort;

    @Override
    @Transactional
    public QuestionLearningOutcomeOperationResponse addLearningOutcomes(AddQuestionLearningOutcomesCommand command) {
        UserAuth user = loadSignedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("User not authenticated"));

        Quiz quiz = loadQuizPort.getById(command.quizId())
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with id: " + command.quizId()));

        Question question = loadQuestionPort.getByIdWithoutAnswers(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException("Question not found with id: " + command.questionId()));

        if (!question.getQuizId().equals(quiz.getId())) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        verifyQuizOwnership(user, quiz);

        Set<UUID> requestedIds = new LinkedHashSet<>(command.learningOutcomeIds());
        if (requestedIds.isEmpty()) {
            throw new IllegalArgumentException("At least one learning outcome id is required");
        }

        List<UUID> existingLearningOutcomeIds = loadCourseLearningOutcomePort
                .findExistingLearningOutcomeIdsByCourseIdAndIds(quiz.getCourseId(), new ArrayList<>(requestedIds));
        if (existingLearningOutcomeIds.size() != requestedIds.size()) {
            throw new IllegalArgumentException("Some learning outcomes do not belong to this course");
        }

        saveQuestionLearningOutcomePort.addLearningOutcomes(question.getId(), new ArrayList<>(requestedIds));

        return QuestionLearningOutcomeOperationResponse.builder()
                .questionId(question.getId())
                .quizId(quiz.getId())
                .courseId(quiz.getCourseId())
                .operation("ADD")
                .totalAffected(requestedIds.size())
                .learningOutcomeIds(loadQuestionLearningOutcomePort.findLearningOutcomeIdsByQuestionId(question.getId()))
                .build();
    }

    private void verifyQuizOwnership(UserAuth user, Quiz quiz) {
        boolean isOwner = (user.getRole() == Roles.TEACHER && user.getId().equals(quiz.getTeacherId()))
                || (user.getRole() == Roles.STUDENT && user.getId().equals(quiz.getStudentId()));

        if (!isOwner) {
            throw new IllegalArgumentException("User is not authorized to modify questions in this quiz");
        }
    }
}
