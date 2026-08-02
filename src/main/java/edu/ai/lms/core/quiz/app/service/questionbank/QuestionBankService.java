package edu.ai.lms.core.quiz.app.service.questionbank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItemStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.exception.QuestionBankConflictException;
import edu.ai.lms.core.quiz.app.exception.QuestionBankItemNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuestionBankValidationException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthorizedException;
import edu.ai.lms.core.quiz.app.port.in.command.QuestionBankItemCommand;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.ValidateLearningOutcomesPort;
import edu.ai.lms.core.quiz.app.port.out.ValidateQuestionBankReferencesPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionBankService implements QuestionBankUseCase {
    private static final int MAX_PAGE_SIZE = 100;

    private final LoadSignedInUserPort signedInUserPort;
    private final LoadCourseMembershipPort courseMembershipPort;
    private final LoadQuestionBankItemPort loadItemPort;
    private final SaveQuestionBankItemPort saveItemPort;
    private final ValidateLearningOutcomesPort learningOutcomesPort;
    private final ValidateQuestionBankReferencesPort referencesPort;

    @Override
    @Transactional
    public MutationResult createDraft(UUID courseId, QuestionBankItemCommand command) {
        CurrentActor actor = requireTeacher(courseId);
        validateDraft(courseId, command);
        QuestionBankItem saved = saveItemPort.create(toDraft(courseId, actor.id(), command));
        return new MutationResult(saved, hasActiveDuplicate(courseId, saved.getContent(), saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankPage findPage(QuestionBankSearchCriteria criteria) {
        requireTeacher(criteria.courseId());
        int page = Math.max(criteria.page(), 0);
        int size = Math.min(Math.max(criteria.size(), 1), MAX_PAGE_SIZE);
        return loadItemPort.findPage(new QuestionBankSearchCriteria(
                criteria.courseId(), trimToNull(criteria.query()), criteria.status(), criteria.chapterId(),
                criteria.ccctItemId(), criteria.learningOutcomeId(), criteria.questionType(),
                criteria.sourceType(), page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankItem get(UUID courseId, UUID itemId) {
        requireTeacher(courseId);
        return loadRequired(courseId, itemId);
    }

    @Override
    @Transactional
    public MutationResult updateDraft(UUID courseId, UUID itemId, long version, QuestionBankItemCommand command) {
        requireTeacher(courseId);
        QuestionBankItem current = loadRequired(courseId, itemId);
        requireVersion(current, version);
        requireStatus(current, QuestionBankItemStatus.DRAFT, "Only Draft items can be edited");
        validateDraft(courseId, command);

        QuestionBankItem replacement = toDraft(courseId, current.getCreatedBy(), command);
        replacement.setId(itemId);
        if (!saveItemPort.replaceDraft(replacement, version)) {
            throw staleConflict();
        }
        QuestionBankItem saved = loadRequired(courseId, itemId);
        return new MutationResult(saved, hasActiveDuplicate(courseId, saved.getContent(), itemId));
    }

    @Override
    @Transactional
    public QuestionBankItem markReady(UUID courseId, UUID itemId, long version) {
        CurrentActor actor = requireTeacher(courseId);
        QuestionBankItem item = loadRequired(courseId, itemId);
        requireVersion(item, version);
        requireStatus(item, QuestionBankItemStatus.DRAFT, "Only Draft items can become Ready for use");
        validateReady(courseId, item);
        if (loadItemPort.existsExactDuplicate(courseId, item.getContent(), itemId, true)) {
            throw new QuestionBankConflictException(
                    "An identical Ready for use question already exists in this course");
        }
        item.setReadyBy(actor.id());
        item.setReadyAt(LocalDateTime.now());
        if (!saveItemPort.markReady(item, version)) {
            throw staleConflict();
        }
        return loadRequired(courseId, itemId);
    }

    @Override
    @Transactional
    public QuestionBankItem archive(UUID courseId, UUID itemId, long version) {
        requireTeacher(courseId);
        QuestionBankItem item = loadRequired(courseId, itemId);
        requireVersion(item, version);
        requireStatus(item, QuestionBankItemStatus.READY_FOR_USE, "Only Ready for use items can be archived");
        if (!saveItemPort.archive(item, version)) {
            throw staleConflict();
        }
        return loadRequired(courseId, itemId);
    }

    @Override
    @Transactional
    public MutationResult duplicate(UUID courseId, UUID itemId, long version) {
        CurrentActor actor = requireTeacher(courseId);
        QuestionBankItem source = loadRequired(courseId, itemId);
        requireVersion(source, version);
        QuestionBankItem copy = QuestionBankItem.builder()
                .courseId(courseId)
                .chapterId(source.getChapterId())
                .ccctItemId(source.getCcctItemId())
                .content(source.getContent())
                .questionType(source.getQuestionType())
                .cognitiveLevelCode(source.getCognitiveLevelCode())
                .difficulty(source.getDifficulty())
                .status(QuestionBankItemStatus.DRAFT)
                .sourceType(QuestionBankSourceType.MANUAL)
                .createdBy(actor.id())
                .answers(source.getAnswers().stream()
                        .map(answer -> QuestionBankAnswer.builder()
                                .content(answer.getContent())
                                .correct(answer.isCorrect())
                                .explanation(answer.getExplanation())
                                .build())
                        .toList())
                .learningOutcomeIds(new LinkedHashSet<>(source.getLearningOutcomeIds()))
                .build();
        QuestionBankItem saved = saveItemPort.create(copy);
        return new MutationResult(saved, true);
    }

    @Override
    @Transactional
    public void deleteDraft(UUID courseId, UUID itemId, long version) {
        requireTeacher(courseId);
        QuestionBankItem item = loadRequired(courseId, itemId);
        requireVersion(item, version);
        requireStatus(item, QuestionBankItemStatus.DRAFT, "Only Draft items can be deleted");
        if (!saveItemPort.deleteDraft(item, version)) {
            throw staleConflict();
        }
    }

    private CurrentActor requireTeacher(UUID courseId) {
        CurrentActor actor = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("Authentication is required"));
        if (actor.role() != Roles.TEACHER || !courseMembershipPort.isTeacherInCourse(actor.id(), courseId)) {
            throw new UserNotAuthorizedException("Only a teacher assigned to this course can manage its Question Bank");
        }
        return actor;
    }

    private void validateDraft(UUID courseId, QuestionBankItemCommand command) {
        if (command == null || trimToNull(command.content()) == null) {
            throw new QuestionBankValidationException("Question content is required");
        }
        if (command.questionType() != Question.QuestionType.MULTIPLE_CHOICE) {
            throw new QuestionBankValidationException("MVP supports MULTIPLE_CHOICE questions only");
        }
        List<QuestionBankItemCommand.Answer> answers = safeAnswers(command.answers());
        if (answers.size() > 10) {
            throw new QuestionBankValidationException("A question can have at most 10 answers");
        }
        if (answers.stream().anyMatch(answer -> trimToNull(answer.content()) == null)) {
            throw new QuestionBankValidationException("Saved answers cannot be blank");
        }
        if (answers.stream().filter(QuestionBankItemCommand.Answer::correct).count() > 1) {
            throw new QuestionBankValidationException("A multiple-choice Draft can have at most one correct answer");
        }
        validateReferences(courseId, command.chapterId(), command.ccctItemId(), safeOutcomes(command.learningOutcomeIds()));
    }

    private void validateReady(UUID courseId, QuestionBankItem item) {
        if (item.getChapterId() == null) {
            throw new QuestionBankValidationException("Chapter is required before an item can become Ready for use");
        }
        if (item.getLearningOutcomeIds() == null || item.getLearningOutcomeIds().isEmpty()) {
            throw new QuestionBankValidationException(
                    "At least one Learning Outcome is required before an item can become Ready for use");
        }
        if (item.getAnswers() == null || item.getAnswers().size() < 2) {
            throw new QuestionBankValidationException(
                    "At least two answers are required before an item can become Ready for use");
        }
        if (item.getAnswers().stream().filter(QuestionBankAnswer::isCorrect).count() != 1) {
            throw new QuestionBankValidationException(
                    "Exactly one correct answer is required before an item can become Ready for use");
        }
        validateReferences(courseId, item.getChapterId(), item.getCcctItemId(), item.getLearningOutcomeIds());
    }

    private void validateReferences(UUID courseId, UUID chapterId, UUID ccctItemId, Set<UUID> outcomeIds) {
        if (chapterId != null && !referencesPort.chapterBelongsToCourse(courseId, chapterId)) {
            throw new QuestionBankValidationException("Chapter does not belong to this course");
        }
        if (!outcomeIds.isEmpty() && !learningOutcomesPort.allBelongToCourse(courseId, outcomeIds)) {
            throw new QuestionBankValidationException("One or more Learning Outcomes do not belong to this course");
        }
        if (ccctItemId != null) {
            UUID mappedChapterId = referencesPort.findPublishedExamCcctItemChapterId(courseId, ccctItemId)
                    .orElseThrow(() -> new QuestionBankValidationException(
                            "CCCT item must be LEARN_AND_EXAM in the Published profile of this course"));
            if (chapterId != null && !chapterId.equals(mappedChapterId)) {
                throw new QuestionBankValidationException("CCCT item and Chapter mapping do not match");
            }
        }
    }

    private QuestionBankItem toDraft(UUID courseId, UUID creatorId, QuestionBankItemCommand command) {
        List<QuestionBankAnswer> answers = safeAnswers(command.answers()).stream()
                .map(answer -> QuestionBankAnswer.builder()
                        .content(answer.content().trim())
                        .correct(answer.correct())
                        .explanation(trimToNull(answer.explanation()))
                        .build())
                .toList();
        return QuestionBankItem.builder()
                .courseId(courseId)
                .chapterId(command.chapterId())
                .ccctItemId(command.ccctItemId())
                .content(command.content().trim())
                .questionType(command.questionType())
                .cognitiveLevelCode(trimToNull(command.cognitiveLevelCode()))
                .difficulty(trimToNull(command.difficulty()))
                .status(QuestionBankItemStatus.DRAFT)
                .sourceType(QuestionBankSourceType.MANUAL)
                .createdBy(creatorId)
                .answers(new ArrayList<>(answers))
                .learningOutcomeIds(new LinkedHashSet<>(safeOutcomes(command.learningOutcomeIds())))
                .build();
    }

    private QuestionBankItem loadRequired(UUID courseId, UUID itemId) {
        return loadItemPort.findByCourseIdAndId(courseId, itemId)
                .orElseThrow(() -> new QuestionBankItemNotFoundException("Question Bank item was not found"));
    }

    private void requireVersion(QuestionBankItem item, long version) {
        if (item.getVersion() != version) {
            throw staleConflict();
        }
    }

    private void requireStatus(QuestionBankItem item, QuestionBankItemStatus expected, String message) {
        if (item.getStatus() != expected) {
            throw new QuestionBankConflictException(message);
        }
    }

    private boolean hasActiveDuplicate(UUID courseId, String content, UUID excludedItemId) {
        return loadItemPort.existsExactDuplicate(courseId, content, excludedItemId, false);
    }

    private QuestionBankConflictException staleConflict() {
        return new QuestionBankConflictException("This item was changed by another request; reload and try again");
    }

    private List<QuestionBankItemCommand.Answer> safeAnswers(List<QuestionBankItemCommand.Answer> answers) {
        return answers == null ? List.of() : answers;
    }

    private Set<UUID> safeOutcomes(Set<UUID> outcomeIds) {
        return outcomeIds == null ? Set.of() : outcomeIds;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
