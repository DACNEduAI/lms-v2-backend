package edu.ai.lms.core.quiz.adapter.in.api;

import edu.ai.lms.core.quiz.api.CodeRunnerQuestionApi;
import edu.ai.lms.core.quiz.api.CodeRunnerQuestionContext;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CodeRunnerQuestionApiAdapter implements CodeRunnerQuestionApi {

    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuizPort loadQuizPort;
    private final LoadQuestionBankItemPort loadQuestionBankItemPort;

    @Override
    public Optional<CodeRunnerQuestionContext> findContext(UUID questionId) {
        Optional<CodeRunnerQuestionContext> fromPublished = loadQuestionPort.getByIdWithoutAnswers(questionId).flatMap(question ->
                loadQuizPort.getById(question.getQuizId()).map(quiz -> new CodeRunnerQuestionContext(
                        question.getId(),
                        quiz.getId(),
                        quiz.getCourseId(),
                        quiz.getTeacherId(),
                        question.getType() == null ? null : question.getType().name(),
                        question.getReviewStatus() == null ? null : question.getReviewStatus().name(),
                        quiz.getLifecycleStatus() == null ? null : quiz.getLifecycleStatus().name(),
                        question.getVersion(),
                        null)));
                        
        if (fromPublished.isPresent()) {
            return fromPublished;
        }
        
        Optional<CodeRunnerQuestionContext> fromDraft = loadQuestionBankItemPort.findDraftById(questionId).map(draft -> new CodeRunnerQuestionContext(
                draft.getId(),
                null, // Treat draft quizId as null to bypass mismatch check in CodeRunnerContractService
                draft.getCourseId(),
                draft.getCreatedBy(), // Assuming creator is teacher
                draft.getQuestionType() == null ? null : draft.getQuestionType().name(),
                "NOT_VALIDATED",
                "DRAFT", // Treat draft as DRAFT quiz lifecycle status
                draft.getVersion(),
                draft.getCodeRunnerContractJson()
        ));
        
        if (fromDraft.isPresent()) {
            return fromDraft;
        }

        return loadQuestionBankItemPort.findBankQuestionById(questionId).map(bank -> new CodeRunnerQuestionContext(
                bank.getId(),
                null,
                bank.getCourseId(),
                bank.getCreatedBy(),
                bank.getQuestionType() == null ? null : bank.getQuestionType().name(),
                "VALIDATED",
                "DRAFT",
                bank.getVersion(),
                bank.getCodeRunnerContractJson()
        ));
    }
}
