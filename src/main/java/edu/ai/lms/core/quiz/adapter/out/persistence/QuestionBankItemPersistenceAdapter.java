package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelCode;
import edu.ai.lms.core.quiz.app.domain.questionbank.CognitiveLevelOption;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionBankItemPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuestionBankItemPersistenceAdapter implements LoadQuestionBankItemPort, SaveQuestionBankItemPort {
    private final QuestionBankItemRepository repository;

    @Override
    @Transactional
    public QuestionDraft createDraft(QuestionDraft draft) {
        prepareNewDraft(draft);
        repository.insertDraft(draft);
        insertDraftChildren(draft);
        return loadDraftRequired(draft.getCourseId(), draft.getId());
    }

    @Override
    @Transactional
    public boolean replaceDraft(QuestionDraft draft, long expectedVersion) {
        prepareChildren(draft.getId(), draft.getAnswers(), false);
        normalizeOutcomes(draft);
        if (repository.updateDraft(draft, expectedVersion) == 0) {
            return false;
        }
        repository.deleteDraftAnswers(draft.getId());
        repository.deleteDraftLearningOutcomes(draft.getId());
        insertDraftChildren(draft);
        return true;
    }

    @Override
    @Transactional
    public QuestionBankQuestion createBankQuestion(QuestionBankQuestion question) {
        prepareNewBankQuestion(question);
        repository.insertBankQuestion(question);
        insertBankChildren(question);
        return loadBankRequired(question.getCourseId(), question.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionDraft> findDraftByCourseIdAndId(UUID courseId, UUID draftId) {
        return repository.findDraftByCourseIdAndId(courseId, draftId).map(this::loadDraftChildren);
    }

    @Override
    @Transactional
    public Optional<QuestionDraft> findDraftByCourseIdAndIdForUpdate(UUID courseId, UUID draftId) {
        return repository.findDraftByCourseIdAndIdForUpdate(courseId, draftId).map(this::loadDraftChildren);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionBankQuestion> findBankByCourseIdAndId(UUID courseId, UUID questionId) {
        return repository.findBankByCourseIdAndId(courseId, questionId).map(this::loadBankChildren);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankPage<QuestionDraft> findDraftPage(QuestionBankSearchCriteria criteria) {
        return new QuestionBankPage(repository.findDraftPage(criteria), repository.countDraftPage(criteria),
                criteria.page(), criteria.size());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankPage<QuestionBankQuestion> findBankPage(QuestionBankSearchCriteria criteria) {
        return new QuestionBankPage(repository.findBankPage(criteria), repository.countBankPage(criteria),
                criteria.page(), criteria.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsExactDraftDuplicate(UUID courseId, String content, UUID excludedDraftId) {
        return repository.existsExactDraftDuplicate(courseId, content, excludedDraftId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsExactBankDuplicate(UUID courseId, String content) {
        return repository.existsExactBankDuplicate(courseId, content) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CognitiveLevelOption> findCognitiveLevel(CognitiveLevelCode code) {
        return repository.findCognitiveLevel(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CognitiveLevelOption> findCognitiveLevels() {
        return repository.findCognitiveLevels();
    }

    @Override
    @Transactional
    public boolean deleteDraft(QuestionDraft draft, long expectedVersion) {
        return repository.deleteDraft(draft, expectedVersion) == 1;
    }

    private void prepareNewDraft(QuestionDraft draft) {
        draft.setId(draft.getId() == null ? UUID.randomUUID() : draft.getId());
        draft.setSourceType(draft.getSourceType() == null ? QuestionBankSourceType.MANUAL : draft.getSourceType());
        draft.setVersion(0);
        prepareChildren(draft.getId(), draft.getAnswers(), false);
        normalizeOutcomes(draft);
    }

    private void prepareNewBankQuestion(QuestionBankQuestion question) {
        question.setId(question.getId() == null ? UUID.randomUUID() : question.getId());
        question.setSourceType(question.getSourceType() == null ? QuestionBankSourceType.MANUAL : question.getSourceType());
        question.setVersion(0);
        question.setAnswers(question.getAnswers() == null
                ? new ArrayList<>() : new ArrayList<>(question.getAnswers()));
        prepareChildren(question.getId(), question.getAnswers(), true);
        question.setLearningOutcomeIds(question.getLearningOutcomeIds() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(question.getLearningOutcomeIds()));
    }

    private void prepareChildren(UUID parentId, List<QuestionBankAnswer> sourceAnswers, boolean regenerateIds) {
        List<QuestionBankAnswer> answers = sourceAnswers == null
                ? new ArrayList<>() : sourceAnswers;
        for (int index = 0; index < answers.size(); index++) {
            QuestionBankAnswer answer = answers.get(index);
            if (regenerateIds || answer.getId() == null) {
                answer.setId(UUID.randomUUID());
            }
            answer.setQuestionId(parentId);
            answer.setOrderIndex(index);
        }
    }

    private void normalizeOutcomes(QuestionDraft draft) {
        draft.setAnswers(draft.getAnswers() == null ? new ArrayList<>() : new ArrayList<>(draft.getAnswers()));
        draft.setLearningOutcomeIds(draft.getLearningOutcomeIds() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(draft.getLearningOutcomeIds()));
    }

    private void insertDraftChildren(QuestionDraft draft) {
        if (!draft.getAnswers().isEmpty()) repository.insertDraftAnswers(draft.getAnswers());
        if (!draft.getLearningOutcomeIds().isEmpty()) {
            repository.insertDraftLearningOutcomes(draft.getId(), List.copyOf(draft.getLearningOutcomeIds()));
        }
    }

    private void insertBankChildren(QuestionBankQuestion question) {
        if (!question.getAnswers().isEmpty()) repository.insertBankAnswers(question.getAnswers());
        if (!question.getLearningOutcomeIds().isEmpty()) {
            repository.insertBankLearningOutcomes(question.getId(), List.copyOf(question.getLearningOutcomeIds()));
        }
    }

    private QuestionDraft loadDraftChildren(QuestionDraft draft) {
        draft.setAnswers(new ArrayList<>(repository.findDraftAnswers(draft.getId())));
        draft.setLearningOutcomeIds(new LinkedHashSet<>(repository.findDraftLearningOutcomeIds(draft.getId())));
        return draft;
    }

    private QuestionBankQuestion loadBankChildren(QuestionBankQuestion question) {
        question.setAnswers(new ArrayList<>(repository.findBankAnswers(question.getId())));
        question.setLearningOutcomeIds(new LinkedHashSet<>(repository.findBankLearningOutcomeIds(question.getId())));
        return question;
    }

    private QuestionDraft loadDraftRequired(UUID courseId, UUID itemId) {
        return findDraftByCourseIdAndId(courseId, itemId)
                .orElseThrow(() -> new IllegalStateException("Question Draft disappeared: " + itemId));
    }

    private QuestionBankQuestion loadBankRequired(UUID courseId, UUID itemId) {
        return findBankByCourseIdAndId(courseId, itemId)
                .orElseThrow(() -> new IllegalStateException("Question Bank question disappeared: " + itemId));
    }
}
