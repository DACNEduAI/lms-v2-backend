package edu.ai.lms.core.quiz.app.service.quizworkflow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuestionReviewStatus;
import edu.ai.lms.core.quiz.app.domain.QuestionSourceType;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.exception.QuestionBankValidationException;
import edu.ai.lms.core.quiz.app.exception.QuizLifecycleConflictException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuizWorkflowUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.ManageQuizLifecyclePort;
import edu.ai.lms.core.quiz.app.port.out.SaveAnswerPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionLearningOutcomePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizWorkflowService implements QuizWorkflowUseCase {
    private final LoadSignedInUserPort signedInUserPort;
    private final LoadQuizPort loadQuizPort;
    private final ManageQuizLifecyclePort lifecyclePort;
    private final QuizAccessPolicy accessPolicy;
    private final LoadQuestionBankItemPort loadBankItemPort;
    private final LoadQuestionPort loadQuestionPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final SaveQuestionLearningOutcomePort saveQuestionLearningOutcomePort;

    @Override
    @Transactional
    public List<Question> addBankQuestions(UUID courseId, UUID quizId, long version, List<UUID> bankQuestionIds) {
        CurrentActor actor = requireActor();
        Quiz quiz = loadRequired(courseId, quizId);
        accessPolicy.requireCourseTeacher(actor, quiz);
        QuizAccessPolicy.requireMutable(quiz);
        requireVersion(quiz, version);
        if (bankQuestionIds == null || bankQuestionIds.isEmpty()) {
            throw new QuestionBankValidationException("Select at least one validated Question Bank question");
        }

        List<Question> created = new ArrayList<>();
        for (UUID bankQuestionId : new LinkedHashSet<>(bankQuestionIds)) {
            QuestionBankQuestion item = loadBankItemPort.findBankByCourseIdAndId(courseId, bankQuestionId)
                    .orElseThrow(() -> new QuestionBankValidationException(
                            "Validated Question Bank question does not belong to this course"));
            created.add(copyBankItem(item, quizId));
        }

        if (!lifecyclePort.touchContent(courseId, quizId, version, quiz.getLifecycleStatus())) {
            throw staleConflict();
        }
        return created;
    }

    @Override
    @Transactional
    public Quiz publish(UUID courseId, UUID quizId, long version) {
        CurrentActor actor = requireActor();
        Quiz quiz = loadRequiredWithQuestions(courseId, quizId);
        accessPolicy.requireCourseTeacher(actor, quiz);
        requireVersion(quiz, version);
        QuizLifecycleStatus expectedStatus = quiz.getLifecycleStatus();
        // REVIEW_PENDING is accepted only for backward compatibility with generated
        // drafts already persisted before the source-neutral workflow was adopted.
        if (expectedStatus != QuizLifecycleStatus.DRAFT
                && expectedStatus != QuizLifecycleStatus.REVIEW_PENDING) {
            throw new QuizLifecycleConflictException("Only a Draft Quiz can be published");
        }

        validateQuestions(quiz.getQuestions());
        if (!lifecyclePort.transition(courseId, quizId, version, expectedStatus,
                QuizLifecycleStatus.PUBLISHED, true, actor.id(), LocalDateTime.now())) {
            throw staleConflict();
        }
        return loadRequired(courseId, quizId);
    }

    private Question copyBankItem(QuestionBankQuestion item, UUID quizId) {
        Question question = saveQuestionPort.save(Question.builder()
                .content(item.getContent())
                .type(item.getQuestionType())
                .quizId(quizId)
                .reviewStatus(QuestionReviewStatus.APPROVED)
                .sourceType(QuestionSourceType.BANK_COPY)
                .sourceBankQuestionId(item.getId())
                .version(0)
                .build());
        item.getAnswers().forEach(answer -> saveAnswerPort.save(Answer.builder()
                .questionId(question.getId())
                .content(answer.getContent())
                .isCorrect(answer.isCorrect())
                .explanation(answer.getExplanation())
                .build()));
        if (!item.getLearningOutcomeIds().isEmpty()) {
            saveQuestionLearningOutcomePort.addLearningOutcomes(
                    question.getId(), List.copyOf(item.getLearningOutcomeIds()));
        }
        return loadQuestionPort.getById(question.getId())
                .orElseThrow(() -> new QuizLifecycleConflictException(
                        "Copied question could not be loaded"));
    }

    private void validateQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new QuizLifecycleConflictException("A Quiz needs at least one valid question");
        }
        for (Question question : questions) {
            if (question.getContent() == null || question.getContent().isBlank()
                    || question.getType() == null) {
                throw new QuizLifecycleConflictException(
                        "Every question requires content and a question type");
            }
            if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE
                    || question.getType() == Question.QuestionType.TRUE_FALSE) {
                if (question.getAnswers() == null || question.getAnswers().size() < 2
                        || question.getAnswers().stream().filter(Answer::isCorrect).count() != 1) {
                    throw new QuizLifecycleConflictException(
                            "Every objective question needs at least two answers and exactly one correct answer");
                }
            }
        }
    }

    private CurrentActor requireActor() {
        return signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("Authentication is required"));
    }

    private Quiz loadRequired(UUID courseId, UUID quizId) {
        return loadQuizPort.getById(quizId)
                .filter(quiz -> courseId.equals(quiz.getCourseId()))
                .orElseThrow(() -> new QuizNotFoundException("Quiz was not found in this course"));
    }

    private Quiz loadRequiredWithQuestions(UUID courseId, UUID quizId) {
        return loadQuizPort.getByIdWithQuestionsAndAnswers(courseId, quizId)
                .orElseThrow(() -> new QuizNotFoundException("Quiz was not found in this course"));
    }

    private void requireVersion(Quiz quiz, long version) {
        if (quiz.getVersion() != version) {
            throw staleConflict();
        }
    }

    private QuizLifecycleConflictException staleConflict() {
        return new QuizLifecycleConflictException(
                "Quiz changed in another request; reload and try again");
    }
}
