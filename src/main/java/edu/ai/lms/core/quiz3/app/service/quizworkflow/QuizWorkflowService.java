package edu.ai.lms.core.quiz3.app.service.quizworkflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.quiz3.app.domain.Answer;
import edu.ai.lms.core.quiz3.app.domain.CurrentActor;
import edu.ai.lms.core.quiz3.app.domain.Question;
import edu.ai.lms.core.quiz3.app.domain.Quiz;
import edu.ai.lms.core.quiz3.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz3.app.exception.QuizLifecycleConflictException;
import edu.ai.lms.core.quiz3.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz3.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz3.app.port.in.usecase.QuizWorkflowUseCase;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz3.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz3.app.port.out.ManageQuizLifecyclePort;
import edu.ai.lms.core.quiz3.app.port.out.ManageQuizReleasePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizWorkflowService implements QuizWorkflowUseCase {
    private final LoadSignedInUserPort signedInUserPort;
    private final LoadQuizPort loadQuizPort;
    private final ManageQuizLifecyclePort lifecyclePort;
    private final ManageQuizReleasePort releasePort;
    private final QuizAccessPolicy accessPolicy;
    private final LoadQuestionPort loadQuestionPort;

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
        LocalDateTime publishedAt = LocalDateTime.now();
        if (!lifecyclePort.transition(courseId, quizId, version, expectedStatus,
                QuizLifecycleStatus.PUBLISHED, true, actor.id(), publishedAt)) {
            throw staleConflict();
        }
        releasePort.publishRelease(quizId, version + 1, actor.id(), publishedAt);
        return loadRequired(courseId, quizId);
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
