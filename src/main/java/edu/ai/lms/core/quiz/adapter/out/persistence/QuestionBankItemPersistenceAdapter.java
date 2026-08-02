package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItemStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankPage;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSearchCriteria;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionBankItemPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuestionBankItemPersistenceAdapter implements LoadQuestionBankItemPort, SaveQuestionBankItemPort {
    private final QuestionBankItemRepository repository;

    @Override
    @Transactional
    public QuestionBankItem create(QuestionBankItem item) {
        prepareNewItem(item);
        repository.insertItem(item);
        insertChildren(item);
        return loadRequired(item.getCourseId(), item.getId());
    }

    @Override
    @Transactional
    public boolean replaceDraft(QuestionBankItem item, long expectedVersion) {
        prepareChildren(item);
        if (repository.updateDraft(item, expectedVersion) == 0) {
            return false;
        }
        repository.deleteAnswersByItemId(item.getId());
        repository.deleteLearningOutcomesByItemId(item.getId());
        insertChildren(item);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionBankItem> findByCourseIdAndId(UUID courseId, UUID itemId) {
        return repository.findByCourseIdAndId(courseId, itemId).map(this::loadChildren);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankPage findPage(QuestionBankSearchCriteria criteria) {
        return new QuestionBankPage(
                repository.findPage(criteria),
                repository.countPage(criteria),
                criteria.page(),
                criteria.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsExactDuplicate(UUID courseId, String content, UUID excludedItemId, boolean readyOnly) {
        return repository.existsExactDuplicate(courseId, content, excludedItemId, readyOnly) > 0;
    }

    @Override
    @Transactional
    public boolean markReady(QuestionBankItem item, long expectedVersion) {
        return repository.markReady(item, expectedVersion) == 1;
    }

    @Override
    @Transactional
    public boolean archive(QuestionBankItem item, long expectedVersion) {
        return repository.archive(item, expectedVersion) == 1;
    }

    @Override
    @Transactional
    public boolean deleteDraft(QuestionBankItem item, long expectedVersion) {
        return repository.deleteDraft(item, expectedVersion) == 1;
    }

    private void prepareNewItem(QuestionBankItem item) {
        item.setId(item.getId() == null ? UUID.randomUUID() : item.getId());
        item.setStatus(item.getStatus() == null ? QuestionBankItemStatus.DRAFT : item.getStatus());
        item.setSourceType(item.getSourceType() == null ? QuestionBankSourceType.MANUAL : item.getSourceType());
        item.setVersion(0);
        prepareChildren(item);
    }

    private void prepareChildren(QuestionBankItem item) {
        List<QuestionBankAnswer> answers = item.getAnswers() == null
                ? new ArrayList<>()
                : new ArrayList<>(item.getAnswers());
        for (int index = 0; index < answers.size(); index++) {
            QuestionBankAnswer answer = answers.get(index);
            answer.setId(answer.getId() == null ? UUID.randomUUID() : answer.getId());
            answer.setQuestionBankItemId(item.getId());
            answer.setOrderIndex(index);
        }
        item.setAnswers(answers);
        item.setLearningOutcomeIds(item.getLearningOutcomeIds() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(item.getLearningOutcomeIds()));
    }

    private void insertChildren(QuestionBankItem item) {
        if (!item.getAnswers().isEmpty()) {
            repository.insertAnswers(item.getAnswers());
        }
        if (!item.getLearningOutcomeIds().isEmpty()) {
            repository.insertLearningOutcomes(item.getId(), List.copyOf(item.getLearningOutcomeIds()));
        }
    }

    private QuestionBankItem loadChildren(QuestionBankItem item) {
        item.setAnswers(new ArrayList<>(repository.findAnswersByItemId(item.getId())));
        item.setLearningOutcomeIds(new LinkedHashSet<>(repository.findLearningOutcomeIdsByItemId(item.getId())));
        return item;
    }

    private QuestionBankItem loadRequired(UUID courseId, UUID itemId) {
        return findByCourseIdAndId(courseId, itemId)
                .orElseThrow(() -> new IllegalStateException("Question Bank item disappeared: " + itemId));
    }
}
