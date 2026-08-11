package edu.ai.lms.core.quiz.app.service.assessment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentDraftValidationService {
    private final AssessmentWorkflowRepository workflow;
    private final QuestionBankUseCase questionBank;

    @Transactional
    public void validate(UUID courseId, UUID assessmentId, UUID draftId, long version, int optionCount) {
        QuestionDraft draft = questionBank.getDraft(courseId, draftId);
        validateStructure(draft, optionCount);
        var provenance = workflow.draftProvenance(assessmentId, draftId);
        var saved = questionBank.validateDraft(courseId, draftId, version);
        workflow.attachBankProvenance(assessmentId, saved.getOriginDraftId(), provenance);
    }

    private void validateStructure(QuestionDraft draft, int optionCount) {
        if (draft.getAnswers() == null || draft.getAnswers().size() != optionCount) {
            throw new IllegalStateException("Quiz2 Draft must contain exactly " + optionCount + " answers");
        }
        if (draft.getAnswers().stream().filter(answer -> answer.isCorrect()).count() != 1) {
            throw new IllegalStateException("Quiz2 Draft must contain exactly one correct answer");
        }
        if (draft.getAnswers().stream().anyMatch(answer -> blank(answer.getExplanation()))) {
            throw new IllegalStateException("Every Quiz2 Draft answer requires an explanation");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
