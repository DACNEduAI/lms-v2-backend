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
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
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
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentPolicyService;
import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
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
    private final RichContentPolicyService richContentPolicy;
    private final RichContentAssetRepository richContentAssets;

    @Override
    @Transactional
    public QuestionDraft createDraft(UUID courseId, QuestionBankItemCommand command) {
        CurrentActor actor = requireTeacher(courseId);
        validateDraft(courseId, command);
        return saveItemPort.createDraft(
                toDraft(courseId, actor.id(), QuestionBankSourceType.MANUAL, command));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankPage<QuestionDraft> findDraftPage(QuestionBankSearchCriteria criteria) {
        requireTeacher(criteria.courseId());
        return loadItemPort.findDraftPage(normalize(criteria));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankPage<QuestionBankQuestion> findBankPage(QuestionBankSearchCriteria criteria) {
        requireTeacher(criteria.courseId());
        return loadItemPort.findBankPage(normalize(criteria));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDraft getDraft(UUID courseId, UUID draftId) {
        requireTeacher(courseId);
        return loadDraftRequired(courseId, draftId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankQuestion getBankQuestion(UUID courseId, UUID questionId) {
        requireTeacher(courseId);
        return loadBankRequired(courseId, questionId);
    }

    @Override
    @Transactional
    public QuestionDraft createSuccessorDraft(UUID courseId, UUID questionId) {
        CurrentActor actor = requireTeacher(courseId);
        QuestionBankQuestion source = loadBankRequired(courseId, questionId);
        QuestionDraft successor = QuestionDraft.builder()
                .courseId(courseId).chapterId(source.getChapterId())
                .cognitiveLevelId(source.getCognitiveLevelId()).cognitiveLevelCode(source.getCognitiveLevelCode())
                .cognitiveLevelName(source.getCognitiveLevelName()).content(source.getContent())
                .contentFormat(source.getContentFormat()).explanation(source.getExplanation())
                .explanationFormat(source.getExplanationFormat()).questionType(source.getQuestionType())
                .sourceType(source.getSourceType()).createdBy(actor.id()).supersedesBankQuestionId(source.getId())
                .answers(new ArrayList<>(copyAnswers(source.getAnswers())))
                .learningOutcomeIds(new LinkedHashSet<>(source.getLearningOutcomeIds())).build();
        QuestionDraft saved = saveItemPort.createDraft(successor);
        richContentAssets.copyBankAssetsToDraft(source.getId(), saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public QuestionDraft updateDraft(UUID courseId, UUID draftId, long version, QuestionBankItemCommand command) {
        requireTeacher(courseId);
        QuestionDraft current = loadDraftRequired(courseId, draftId);
        requireVersion(current, version);
        validateDraft(courseId, command);

        QuestionDraft replacement = toDraft(courseId, current.getCreatedBy(), current.getSourceType(), command);
        replacement.setId(draftId);
        if (!saveItemPort.replaceDraft(replacement, version)) throw staleConflict();
        return loadDraftRequired(courseId, draftId);
    }

    @Override
    @Transactional
    public QuestionBankQuestion validateDraft(UUID courseId, UUID draftId, long version) {
        CurrentActor actor = requireTeacher(courseId);
        QuestionDraft draft = loadItemPort.findDraftByCourseIdAndIdForUpdate(courseId, draftId)
                .orElseThrow(() -> new QuestionBankItemNotFoundException("Question Draft was not found"));
        requireVersion(draft, version);
        validateForBank(courseId, draft);
        QuestionBankQuestion bankQuestion = QuestionBankQuestion.builder()
                .originDraftId(draft.getId())
                .courseId(courseId)
                .chapterId(draft.getChapterId())
                .cognitiveLevelId(draft.getCognitiveLevelId())
                .cognitiveLevelCode(draft.getCognitiveLevelCode())
                .cognitiveLevelName(draft.getCognitiveLevelName())
                .content(draft.getContent())
                .contentFormat(draft.getContentFormat())
                .explanation(draft.getExplanation())
                .explanationFormat(draft.getExplanationFormat())
                .questionType(draft.getQuestionType())
                .sourceType(draft.getSourceType())
                .createdBy(draft.getCreatedBy())
                .supersedesBankQuestionId(draft.getSupersedesBankQuestionId())
                .validatedBy(actor.id())
                .validatedAt(LocalDateTime.now())
                .createdAt(draft.getCreatedAt())
                .answers(copyAnswers(draft.getAnswers()))
                .learningOutcomeIds(new LinkedHashSet<>(draft.getLearningOutcomeIds()))
                .build();
        QuestionBankQuestion saved = saveItemPort.createBankQuestion(bankQuestion);
        richContentAssets.copyDraftAssetsToBank(draft.getId(), saved.getId());
        if (!saveItemPort.deleteDraft(draft, version)) throw staleConflict();
        return saved;
    }

    @Override
    @Transactional
    public void deleteDraft(UUID courseId, UUID draftId, long version) {
        requireTeacher(courseId);
        QuestionDraft draft = loadDraftRequired(courseId, draftId);
        requireVersion(draft, version);
        if (!saveItemPort.deleteDraft(draft, version)) throw staleConflict();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CognitiveLevelOption> getCognitiveLevels(UUID courseId) {
        requireTeacher(courseId);
        return loadItemPort.findCognitiveLevels();
    }

    private CurrentActor requireTeacher(UUID courseId) {
        CurrentActor actor = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("Authentication is required"));
        if (actor.role() != Roles.TEACHER || !courseMembershipPort.isTeacherInCourse(actor.id(), courseId)) {
            throw new UserNotAuthorizedException("Only a teacher assigned to this course can manage its Question Bank");
        }
        return actor;
    }

    private QuestionBankSearchCriteria normalize(QuestionBankSearchCriteria criteria) {
        int page = Math.max(criteria.page(), 0);
        int size = Math.min(Math.max(criteria.size(), 1), MAX_PAGE_SIZE);
        return new QuestionBankSearchCriteria(
                criteria.courseId(), trimToNull(criteria.query()), criteria.chapterId(),
                criteria.learningOutcomeId(), criteria.questionType(), criteria.cognitiveLevelCode(),
                criteria.sourceType(), page, size);
    }

    private void validateDraft(UUID courseId, QuestionBankItemCommand command) {
        if (command == null || !richContentPolicy.hasMeaningfulContent(toRichText(command.content(), command.contentFormat()))) {
            throw new QuestionBankValidationException("Question content is required");
        }
        if (command.questionType() != Question.QuestionType.MULTIPLE_CHOICE) {
            throw new QuestionBankValidationException("MVP supports MULTIPLE_CHOICE questions only");
        }
        List<QuestionBankItemCommand.Answer> answers = safeAnswers(command.answers());
        if (!Set.of(4, 5).contains(answers.size())) {
            throw new QuestionBankValidationException("MVP multiple-choice questions require exactly four or five answers");
        }
        if (answers.stream().anyMatch(answer -> !richContentPolicy.hasMeaningfulContent(
                toRichText(answer.content(), answer.contentFormat())))) {
            throw new QuestionBankValidationException("Saved answers cannot be blank");
        }
        if (answers.stream().filter(QuestionBankItemCommand.Answer::correct).count() > 1) {
            throw new QuestionBankValidationException("A multiple-choice Draft can have at most one correct answer");
        }
        validateReferences(courseId, command.chapterId(), safeOutcomes(command.learningOutcomeIds()));
        resolveCognitiveLevel(command.cognitiveLevelCode());
    }

    private void validateForBank(UUID courseId, QuestionDraft item) {
        if (item.getChapterId() == null) {
            throw new QuestionBankValidationException("Chapter is required before validation");
        }
        if (item.getLearningOutcomeIds() == null || item.getLearningOutcomeIds().isEmpty()) {
            throw new QuestionBankValidationException("At least one Learning Outcome is required before validation");
        }
        if (item.getCognitiveLevelId() == null) {
            throw new QuestionBankValidationException("Cognitive level is required before validation");
        }
        if (item.getAnswers() == null || !Set.of(4, 5).contains(item.getAnswers().size())) {
            throw new QuestionBankValidationException("Validated multiple-choice questions require exactly four or five answers");
        }
        if (!richContentPolicy.hasMeaningfulContent(toRichText(item.getContent(), item.getContentFormat()))
                || item.getAnswers().stream().anyMatch(answer -> !richContentPolicy.hasMeaningfulContent(
                        toRichText(answer.getContent(), answer.getContentFormat())))) {
            throw new QuestionBankValidationException("Validated answers cannot be blank");
        }
        if (item.getAnswers().stream().filter(QuestionBankAnswer::isCorrect).count() != 1) {
            throw new QuestionBankValidationException("Exactly one correct answer is required before validation");
        }
        validateReferences(courseId, item.getChapterId(), item.getLearningOutcomeIds());
    }

    private void validateReferences(UUID courseId, UUID chapterId, Set<UUID> outcomeIds) {
        if (chapterId != null && !referencesPort.chapterBelongsToCourse(courseId, chapterId)) {
            throw new QuestionBankValidationException("Chapter does not belong to this course");
        }
        if (!outcomeIds.isEmpty() && !learningOutcomesPort.allBelongToCourse(courseId, outcomeIds)) {
            throw new QuestionBankValidationException("One or more Learning Outcomes do not belong to this course");
        }
    }

    private QuestionDraft toDraft(UUID courseId, UUID creatorId, QuestionBankSourceType sourceType,
            QuestionBankItemCommand command) {
        CognitiveLevelOption cognitiveLevel = resolveCognitiveLevel(command.cognitiveLevelCode());
        RichTextValue questionContent = sanitizeRequired(command.content(), command.contentFormat(),
                RichContentPolicyService.MAX_PROMPT_HTML_LENGTH, "Question content is required");
        RichTextValue questionExplanation = sanitizeOptional(command.explanation(), command.explanationFormat(),
                RichContentPolicyService.MAX_ANSWER_HTML_LENGTH);
        List<QuestionBankAnswer> answers = safeAnswers(command.answers()).stream()
                .map(answer -> {
                    RichTextValue answerContent = sanitizeRequired(answer.content(), answer.contentFormat(),
                            RichContentPolicyService.MAX_ANSWER_HTML_LENGTH, "Saved answers cannot be blank");
                    return QuestionBankAnswer.builder()
                        .content(answerContent.content()).contentFormat(answerContent.contentFormat())
                        .correct(answer.correct())
                        .explanation(null).explanationFormat(RichContentFormat.PLAIN_TEXT)
                        .build();
                })
                .toList();
        return QuestionDraft.builder()
                .courseId(courseId)
                .chapterId(command.chapterId())
                .cognitiveLevelId(cognitiveLevel == null ? null : cognitiveLevel.id())
                .cognitiveLevelCode(cognitiveLevel == null ? null : cognitiveLevel.code())
                .cognitiveLevelName(cognitiveLevel == null ? null : cognitiveLevel.name())
                .content(questionContent.content()).contentFormat(questionContent.contentFormat())
                .explanation(questionExplanation.content().isBlank() ? null : questionExplanation.content())
                .explanationFormat(questionExplanation.contentFormat())
                .questionType(command.questionType())
                .sourceType(sourceType)
                .createdBy(creatorId)
                .answers(new ArrayList<>(answers))
                .learningOutcomeIds(new LinkedHashSet<>(safeOutcomes(command.learningOutcomeIds())))
                .build();
    }

    private CognitiveLevelOption resolveCognitiveLevel(
            edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode code) {
        if (code == null) return null;
        return loadItemPort.findCognitiveLevel(code)
                .orElseThrow(() -> new QuestionBankValidationException("Cognitive level is not supported"));
    }

    private List<QuestionBankAnswer> copyAnswers(List<QuestionBankAnswer> answers) {
        return answers.stream().map(answer -> QuestionBankAnswer.builder()
                .content(answer.getContent()).contentFormat(answer.getContentFormat()).correct(answer.isCorrect())
                .explanation(answer.getExplanation()).explanationFormat(answer.getExplanationFormat()).build()).toList();
    }

    private QuestionDraft loadDraftRequired(UUID courseId, UUID draftId) {
        return loadItemPort.findDraftByCourseIdAndId(courseId, draftId)
                .orElseThrow(() -> new QuestionBankItemNotFoundException("Question Draft was not found"));
    }

    private QuestionBankQuestion loadBankRequired(UUID courseId, UUID questionId) {
        return loadItemPort.findBankByCourseIdAndId(courseId, questionId)
                .orElseThrow(() -> new QuestionBankItemNotFoundException("Question Bank question was not found"));
    }

    private void requireVersion(QuestionDraft item, long version) {
        if (item.getVersion() != version) throw staleConflict();
    }

    private QuestionBankConflictException staleConflict() {
        return new QuestionBankConflictException("This Draft was changed by another request; reload and try again");
    }

    private List<QuestionBankItemCommand.Answer> safeAnswers(List<QuestionBankItemCommand.Answer> answers) {
        return answers == null ? List.of() : answers;
    }

    private Set<UUID> safeOutcomes(Set<UUID> outcomeIds) {
        return outcomeIds == null ? Set.of() : outcomeIds;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RichTextValue toRichText(String content, RichContentFormat format) {
        return new RichTextValue(content, format == null ? RichContentFormat.PLAIN_TEXT : format);
    }

    private RichTextValue sanitizeRequired(String content, RichContentFormat format, int maxLength, String message) {
        RichTextValue sanitized = richContentPolicy.sanitize(toRichText(content, format), maxLength);
        if (!richContentPolicy.hasMeaningfulContent(sanitized)) throw new QuestionBankValidationException(message);
        return sanitized.contentFormat() == RichContentFormat.PLAIN_TEXT
                ? new RichTextValue(sanitized.content().trim(), RichContentFormat.PLAIN_TEXT) : sanitized;
    }

    private RichTextValue sanitizeOptional(String content, RichContentFormat format, int maxLength) {
        RichTextValue sanitized = richContentPolicy.sanitize(toRichText(content, format), maxLength);
        if (!richContentPolicy.hasMeaningfulContent(sanitized)) return RichTextValue.plainText("");
        return sanitized.contentFormat() == RichContentFormat.PLAIN_TEXT
                ? new RichTextValue(sanitized.content().trim(), RichContentFormat.PLAIN_TEXT) : sanitized;
    }
}
