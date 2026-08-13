package edu.ai.lms.core.quiz.app.service.assessment;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.QuestionAiRepository;
import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository.AnswerInput;
import edu.ai.lms.core.quiz.app.domain.CurrentActor;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankQuestion;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthorizedException;
import edu.ai.lms.core.quiz.app.exception.AssessmentValidationException;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.quiz.app.service.richcontent.RichContentPolicyService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AssessmentService {
    private final AssessmentRepository assessments;
    private final AssessmentWorkflowRepository workflow;
    private final QuestionBankUseCase questionBank;
    private final LoadSignedInUserPort signedInUserPort;
    private final LoadCourseMembershipPort membershipPort;
    private final AssessmentGenerationDispatcher dispatcher;
    private final AssessmentDraftValidationService draftValidator;
    private final ObjectMapper objectMapper;
    private final RichContentPolicyService richContentPolicy;
    private final QuestionAiRepository questionAi;

    public AssessmentService(AssessmentRepository assessments, AssessmentWorkflowRepository workflow,
            QuestionBankUseCase questionBank, LoadSignedInUserPort signedInUserPort,
            LoadCourseMembershipPort membershipPort, AssessmentGenerationDispatcher dispatcher,
            AssessmentDraftValidationService draftValidator, ObjectMapper objectMapper,
            RichContentPolicyService richContentPolicy) {
        this(assessments, workflow, questionBank, signedInUserPort, membershipPort, dispatcher,
                draftValidator, objectMapper, richContentPolicy, null);
    }

    public List<AssessmentView> list(UUID courseId) {
        requireTeacher(courseId);
        return assessments.list(courseId).stream()
                .map(assessment -> withQuestionProgress(assessment, workflow.questionProgress(assessment))).toList();
    }

    public AssessmentView get(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        return withQuestionProgress(assessment, workflow.questionProgress(assessment));
    }

    public AssessmentView create(UUID courseId, AssessmentConfiguration input) {
        CurrentActor actor = requireTeacher(courseId);
        validateConfiguration(input);
        return assessments.create(courseId, actor.id(), input.title().trim(), trimToNull(input.description()),
                input.unlimited(), input.timeLimitMinutes(), input.passScore(), input.startTime(), input.dueTime(),
                input.attemptLimited(), input.variantCount(), input.optionCount(),
                input.allowRecentlyUsedQuestions());
    }

    public AssessmentView update(UUID courseId, UUID assessmentId, long version, AssessmentConfiguration input) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView current = required(courseId, assessmentId);
        if (current.lifecycleStatus() == LifecycleStatus.PUBLISHED) {
            throw new AssessmentValidationException("ASSESSMENT_READ_ONLY", "configuration", null, null,
                    "Assessment is published and read-only; unpublish it before editing");
        }
        validateConfiguration(input);
        if (!assessments.update(courseId, assessmentId, version, input.title().trim(), trimToNull(input.description()),
                input.unlimited(), input.timeLimitMinutes(), input.passScore(), input.startTime(), input.dueTime(),
                input.attemptLimited(), input.variantCount(), input.optionCount(),
                input.allowRecentlyUsedQuestions(), actor.id())) {
            throw new IllegalStateException("Assessment changed before this update; reload and try again");
        }
        return required(courseId, assessmentId);
    }

    public ContextView context(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        return new ContextView(required(courseId, assessmentId), assessments.listCcctProfiles(courseId),
                assessments.listExamMatrices(courseId));
    }

    @Transactional
    public ContextView selectContext(UUID courseId, UUID assessmentId, ContextSelection input) {
        CurrentActor actor = requireTeacher(courseId);
        if (input == null || input.ccct() == null || input.examMatrix() == null) {
            throw new IllegalArgumentException("CCCT and Exam Matrix selections are required");
        }
        UUID ccctId = resolveCcct(courseId, actor.id(), input.ccct());
        UUID matrixId = resolveMatrix(courseId, input.examMatrix());
        assessments.selectSources(courseId, assessmentId, ccctId, matrixId);
        return context(courseId, assessmentId);
    }

    private UUID resolveCcct(UUID courseId, UUID actorId, SourceSelection selection) {
        return switch (selection.mode()) {
            case USE_EXISTING -> requireSourceId(selection, "CCCT");
            case COPY_AS_DRAFT -> assessments.cloneCcct(
                    courseId, requireSourceId(selection, "CCCT"), actorId);
            case CREATE_NEW_DRAFT -> assessments.createCcctDraft(
                    courseId, requireDraftName(selection, "CCCT"), actorId);
        };
    }

    private UUID resolveMatrix(UUID courseId, SourceSelection selection) {
        return switch (selection.mode()) {
            case USE_EXISTING -> requireSourceId(selection, "Exam Matrix");
            case COPY_AS_DRAFT -> assessments.cloneMatrix(
                    courseId, requireSourceId(selection, "Exam Matrix"));
            case CREATE_NEW_DRAFT -> assessments.createMatrixDraft(
                    courseId, requireDraftName(selection, "Exam Matrix"));
        };
    }

    private UUID requireSourceId(SourceSelection selection, String source) {
        if (selection.sourceId() == null) {
            throw new IllegalArgumentException(source + " source is required");
        }
        return selection.sourceId();
    }

    private String requireDraftName(SourceSelection selection, String source) {
        String name = trimToNull(selection.draftName());
        if (name == null) throw new IllegalArgumentException(source + " Draft name is required");
        if (name.length() > 255) throw new IllegalArgumentException(source + " Draft name cannot exceed 255 characters");
        return name;
    }

    public AssessmentView confirmCcct(UUID courseId, UUID assessmentId, long sourceVersion) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.ccctProfileId() == null) throw new IllegalStateException("Select a CCCT first");
        assessments.publishCcct(courseId, assessmentId, assessment.ccctProfileId(), sourceVersion, actor.id());
        return required(courseId, assessmentId);
    }

    public AssessmentView confirmMatrix(UUID courseId, UUID assessmentId, long sourceVersion) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.examMatrixId() == null) throw new IllegalStateException("Select an Exam Matrix first");
        assessments.publishMatrix(courseId, assessmentId, assessment.examMatrixId(), sourceVersion, actor.id());
        return required(courseId, assessmentId);
    }

    public AssessmentView rollbackSources(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        assessments.rollbackSources(courseId, assessmentId);
        return required(courseId, assessmentId);
    }

    public BankPlan bankPlan(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        return workflow.bankPlan(required(courseId, assessmentId));
    }

    @Transactional
    public BatchView generateMissing(UUID courseId, UUID assessmentId) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        BatchView active = workflow.activeBatch(courseId, assessmentId);
        if (active != null) return active;
        if (assessment.currentStep() < 5) throw new IllegalStateException("Publish CCCT and Exam Matrix before generation");
        List<String> unavailable = assessments.unreadyMaterialNames(courseId);
        if (!unavailable.isEmpty()) {
            throw new IllegalStateException("Materials are not ready for AI generation: " + String.join(", ", unavailable));
        }
        List<UUID> materialIds = assessments.allMaterialIds(courseId);
        if (materialIds.isEmpty()) throw new IllegalStateException("The course has no source materials");
        BankPlan plan = workflow.bankPlan(assessment);
        String snapshot = generationSnapshot(assessment, plan, materialIds);
        BatchView batch = workflow.createBatch(assessment, plan, actor.id(), snapshot);
        if (plan.missing() > 0) {
            assessments.markGenerationStarted(courseId, assessmentId);
            dispatcher.dispatch(batch.id());
        }
        return batch;
    }

    public List<BatchView> batches(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        required(courseId, assessmentId);
        return workflow.listBatches(courseId, assessmentId);
    }

    public BatchView getBatch(UUID courseId, UUID assessmentId, UUID batchId) {
        requireTeacher(courseId);
        required(courseId, assessmentId);
        return workflow.getBatch(courseId, assessmentId, batchId);
    }

    public BatchView retryCell(UUID courseId, UUID assessmentId, UUID batchId, UUID cellId) {
        requireTeacher(courseId);
        required(courseId, assessmentId);
        String snapshot = workflow.prepareCellRetry(courseId, assessmentId, batchId, cellId);
        dispatcher.retryCell(batchId, snapshot);
        return workflow.getBatch(courseId, assessmentId, batchId);
    }

    @Transactional
    public BatchView regenerateDraft(UUID courseId, UUID assessmentId, UUID draftId, long version) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        var provenance = workflow.draftProvenance(assessmentId, draftId);
        BankPlan plan = workflow.bankPlan(assessment);
        BankPlanCell source = plan.cells().stream()
                .filter(cell -> cell.matrixRowId().equals(provenance.matrixRowId())
                        && cell.customLearningLevelId().equals(provenance.customLearningLevelId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("The Draft Matrix cell no longer exists"));
        List<String> unavailable = assessments.unreadyMaterialNames(courseId);
        if (!unavailable.isEmpty()) {
            throw new IllegalStateException("Materials are not ready for AI generation: " + String.join(", ", unavailable));
        }
        List<UUID> materialIds = assessments.allMaterialIds(courseId);
        if (materialIds.isEmpty()) throw new IllegalStateException("The course has no source materials");

        questionBank.deleteDraft(courseId, draftId, version);
        BankPlanCell replacement = new BankPlanCell(source.matrixRowId(), source.customLearningLevelId(),
                source.chapterId(), source.chapter(), source.topic(), source.levelCode(), source.levelName(),
                source.learningOutcomeIds(), 1, 0, 0, 0, 0, 1);
        BankPlan replacementPlan = new BankPlan(assessment.id(), assessment.examMatrixId(),
                assessment.allowRecentlyUsedQuestions(), 1, 0, 0, 0, 0, 1, List.of(replacement));
        String snapshot = generationSnapshot(assessment, replacementPlan, materialIds);
        BatchView batch = workflow.createBatch(assessment, replacementPlan, actor.id(), snapshot);
        assessments.markGenerationStarted(courseId, assessmentId);
        dispatcher.dispatch(batch.id());
        return batch;
    }

    public DraftReviewView review(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        required(courseId, assessmentId);
        return workflow.review(assessmentId);
    }

    public DraftReviewPage reviewPage(UUID courseId, UUID assessmentId, DraftReviewSearch search) {
        requireTeacher(courseId);
        required(courseId, assessmentId);
        int page = Math.max(0, search.page());
        int size = List.of(10, 25, 50).contains(search.size()) ? search.size() : 25;
        String status = trimToNull(search.status());
        if (status != null && !List.of("NOT_VALIDATED", "VALIDATED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Unsupported review status");
        }
        return workflow.reviewPage(assessmentId, new DraftReviewSearch(trimToNull(search.query()), status,
                search.chapterId(), search.learningOutcomeId(), trimToNull(search.cognitiveLevelCode()),
                search.generationBatchId(), trimToNull(search.questionType()), page, size));
    }

    public DraftReviewCriteria reviewCriteria(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        return workflow.reviewCriteria(required(courseId, assessmentId));
    }

    public DraftReviewView reject(UUID courseId, UUID assessmentId, UUID draftId, long version, String reason) {
        CurrentActor actor = requireTeacher(courseId);
        required(courseId, assessmentId);
        if (trimToNull(reason) == null) throw new IllegalArgumentException("A rejection reason is required");
        workflow.rejectDraft(assessmentId, draftId, version, actor.id(), reason.trim());
        return workflow.review(assessmentId);
    }

    public DraftValidationResult validate(UUID courseId, UUID assessmentId, UUID draftId, long version) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        draftValidator.validate(courseId, assessmentId, draftId, version, assessment.optionCount());
        return new DraftValidationResult(draftId, "VALIDATED", null);
    }

    public BatchValidationResult validateBatch(UUID courseId, UUID assessmentId, List<DraftVersion> drafts) {
        if (drafts == null || drafts.isEmpty()) throw new IllegalArgumentException("Select at least one Draft");
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        List<DraftValidationResult> results = new ArrayList<>();
        for (DraftVersion draft : drafts) {
            try {
                draftValidator.validate(courseId, assessmentId, draft.id(), draft.version(), assessment.optionCount());
                results.add(new DraftValidationResult(draft.id(), "VALIDATED", null));
            } catch (RuntimeException ex) {
                String message = ex.getMessage() == null ? "Draft validation failed" : ex.getMessage();
                results.add(new DraftValidationResult(draft.id(), "FAILED", message));
            }
        }
        int validated = (int) results.stream().filter(result -> "VALIDATED".equals(result.status())).count();
        return new BatchValidationResult(results, validated, results.size() - validated);
    }

    public int resetRecentlyUsed(UUID courseId, List<UUID> questionIds) {
        requireTeacher(courseId);
        return workflow.resetRecentlyUsed(courseId, questionIds);
    }

    public List<VariantView> createVariants(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        BankPlan plan = workflow.bankPlan(assessment);
        if (plan.missing() > 0) throw new IllegalStateException("Question Bank is missing " + plan.missing() + " questions");
        return workflow.createVariants(assessment, plan);
    }

    public List<VariantView> variants(UUID courseId, UUID assessmentId) {
        requireTeacher(courseId);
        required(courseId, assessmentId);
        return workflow.variants(assessmentId);
    }

    public List<VariantView> recalculateCoverage(UUID courseId, UUID assessmentId, UUID variantId) {
        requireTeacher(courseId);
        workflow.recalculateCoverage(required(courseId, assessmentId), variantId);
        return workflow.variants(assessmentId);
    }

    public List<VariantView> overrideCoverage(UUID courseId, UUID assessmentId, UUID variantId, String reason) {
        CurrentActor actor = requireTeacher(courseId);
        required(courseId, assessmentId);
        if (trimToNull(reason) == null) throw new IllegalArgumentException("An override reason is required");
        workflow.setOverride(assessmentId, variantId, actor.id(), reason.trim());
        return workflow.variants(assessmentId);
    }

    public List<VariantView> updateVariantQuestion(UUID courseId, UUID assessmentId, UUID variantId,
            UUID questionId, VariantQuestionInput input) {
        VariantEditingContext editing = requireVariantEditingContext(courseId, assessmentId, variantId, questionId);
        AssessmentView assessment = editing.assessment();
        VariantQuestionInput sanitized = sanitizeVariantQuestionInput(input, assessment.optionCount());
        workflow.updateVariantQuestion(assessmentId, variantId, questionId, sanitized.content(), sanitized.contentFormat(),
                sanitized.explanation(), sanitized.explanationFormat(), sanitized.answers());
        workflow.recalculateCoverage(required(courseId, assessmentId), variantId);
        return workflow.variants(assessmentId);
    }

    public List<VariantView> updateVariantQuestion(UUID courseId, UUID assessmentId, UUID variantId,
            UUID questionId, long expectedQuestionVersion, VariantQuestionInput input) {
        VariantEditingContext editing = requireVariantEditingContext(courseId, assessmentId, variantId, questionId);
        VariantQuestionInput sanitized = sanitizeVariantQuestionInput(input, editing.assessment().optionCount());
        workflow.updateVariantQuestion(assessmentId, variantId, questionId, expectedQuestionVersion,
                sanitized.content(), sanitized.contentFormat(), sanitized.explanation(),
                sanitized.explanationFormat(), sanitized.answers());
        workflow.recalculateCoverage(required(courseId, assessmentId), variantId);
        return workflow.variants(assessmentId);
    }

    public VariantEditingContext requireVariantEditingContext(UUID courseId, UUID assessmentId, UUID variantId,
            UUID questionId) {
        requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.lifecycleStatus() == LifecycleStatus.PUBLISHED) {
            throw new IllegalStateException("Assessment is published and read-only; unpublish it before editing");
        }
        if (assessment.lifecycleStatus() != LifecycleStatus.READY_TO_PUBLISH) {
            throw new IllegalStateException("Variant content can only be edited at the review step");
        }
        VariantView variant = workflow.variants(assessmentId).stream()
                .filter(value -> value.id().equals(variantId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assessment variant was not found"));
        VariantQuestionView question = variant.questions().stream()
                .filter(value -> value.id().equals(questionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Variant question was not found"));
        return new VariantEditingContext(assessment, variant, question);
    }

    public VariantQuestionInput sanitizeVariantQuestionInput(VariantQuestionInput input, int optionCount) {
        if (input == null) throw new IllegalArgumentException("Question draft is required");
        RichTextValue question = sanitizeRequired(input.content(), input.contentFormat(),
                RichContentPolicyService.MAX_PROMPT_HTML_LENGTH, "Question content is required");
        RichTextValue explanation = sanitizeOptional(input.explanation(), input.explanationFormat(),
                RichContentPolicyService.MAX_ANSWER_HTML_LENGTH);
        List<AnswerInput> answers = input.answers() == null ? List.of() : input.answers();
        if (answers.size() != optionCount) {
            throw new IllegalArgumentException("This Assessment requires exactly " + optionCount + " answers");
        }
        if (answers.stream().filter(AnswerInput::correct).count() != 1) {
            throw new IllegalArgumentException("MCQ requires exactly one correct answer");
        }
        List<AnswerInput> sanitizedAnswers = answers.stream().map(this::sanitizeAnswer).toList();
        return new VariantQuestionInput(question.content(), question.contentFormat(),
                explanation.content().isBlank() ? null : explanation.content(), explanation.contentFormat(), sanitizedAnswers);
    }

    public PublishResult publish(UUID courseId, UUID assessmentId) {
        CurrentActor actor = requireTeacher(courseId);
        if (questionAi != null && questionAi.hasBlockingDraftOrJob(assessmentId)) {
            throw new IllegalStateException("Publish is blocked by AI work on questions: "
                    + questionAi.blockingQuestionIds(assessmentId)
                    + ". Save/discard their working drafts and finish or cancel active jobs.");
        }
        return workflow.publish(required(courseId, assessmentId), actor.id());
    }

    @Transactional
    public AssessmentView unpublish(UUID courseId, UUID assessmentId) {
        CurrentActor actor = requireTeacher(courseId);
        AssessmentView assessment = required(courseId, assessmentId);
        if (assessment.lifecycleStatus() != LifecycleStatus.PUBLISHED) {
            throw new IllegalStateException("Only a published Assessment can be unpublished");
        }
        workflow.unpublish(assessment, actor.id());
        return required(courseId, assessmentId);
    }

    private AssessmentView required(UUID courseId, UUID assessmentId) {
        return assessments.get(courseId, assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment was not found"));
    }

    private AssessmentView withQuestionProgress(AssessmentView assessment, AssessmentQuestionProgress progress) {
        return new AssessmentView(assessment.id(), assessment.courseId(), assessment.title(), assessment.description(),
                assessment.unlimited(), assessment.timeLimitMinutes(), assessment.passScore(), assessment.startTime(),
                assessment.dueTime(), assessment.attemptLimited(), assessment.variantCount(), assessment.optionCount(),
                assessment.allowRecentlyUsedQuestions(), assessment.lifecycleStatus(), assessment.currentStep(),
                assessment.ccctProfileId(), assessment.ccctProfileVersion(), assessment.examMatrixId(),
                assessment.examMatrixVersion(), assessment.aiGenerationStarted(), assessment.version(), assessment.createdBy(),
                assessment.publishedBy(), assessment.publishedAt(), assessment.createdAt(), assessment.updatedAt(), progress);
    }

    private CurrentActor requireTeacher(UUID courseId) {
        CurrentActor actor = signedInUserPort.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotAuthenticatedException("Authentication is required"));
        if (actor.role() != Roles.TEACHER || !membershipPort.isTeacherInCourse(actor.id(), courseId)) {
            throw new UserNotAuthorizedException("Only a teacher assigned to this course can manage Quiz2 assessments");
        }
        return actor;
    }

    private String generationSnapshot(AssessmentView assessment, BankPlan plan, List<UUID> materialIds) {
        List<Map<String, Object>> cells = plan.cells().stream().filter(cell -> cell.missing() > 0).map(cell -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("matrixRowId", cell.matrixRowId());
            value.put("customLearningLevelId", cell.customLearningLevelId());
            value.put("chapterId", cell.chapterId());
            value.put("chapter", cell.chapter());
            value.put("topic", cell.topic());
            value.put("levelCode", cell.levelCode());
            value.put("levelName", cell.levelName());
            value.put("learningOutcomeIds", cell.learningOutcomeIds());
            value.put("count", cell.missing());
            return value;
        }).toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractVersion", 1);
        snapshot.put("assessmentId", assessment.id());
        snapshot.put("courseId", assessment.courseId());
        snapshot.put("courseTitle", assessments.courseTitle(assessment.courseId()));
        snapshot.put("optionCount", assessment.optionCount());
        snapshot.put("questionType", "MULTIPLE_CHOICE");
        snapshot.put("materialIds", materialIds);
        snapshot.put("cells", cells);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not snapshot the generation request", ex);
        }
    }

    private void validateConfiguration(AssessmentConfiguration input) {
        if (input == null || trimToNull(input.title()) == null) throw new IllegalArgumentException("Assessment title is required");
        if (input.variantCount() < 1 || input.variantCount() > 20) throw new IllegalArgumentException("Variant count must be between 1 and 20");
        if (!List.of(4, 5).contains(input.optionCount())) throw new IllegalArgumentException("Option count must be four or five");
        if (!input.unlimited() && (input.timeLimitMinutes() == null || input.timeLimitMinutes() <= 0)) {
            throw new IllegalArgumentException("A positive time limit is required when time is limited");
        }
        if (input.passScore() != null && (input.passScore() < 0 || input.passScore() > 100)) {
            throw new IllegalArgumentException("Pass score must be between 0 and 100");
        }
        if (input.attemptLimited() != null && input.attemptLimited() < 1) throw new IllegalArgumentException("Attempts must be positive");
        if (input.startTime() != null && input.dueTime() != null && !input.dueTime().isAfter(input.startTime())) {
            throw new IllegalArgumentException("Due time must be after start time");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record AssessmentConfiguration(String title, String description, boolean unlimited,
            Integer timeLimitMinutes, Double passScore, LocalDateTime startTime, LocalDateTime dueTime,
            Integer attemptLimited, int variantCount, int optionCount, boolean allowRecentlyUsedQuestions) {
    }

    public enum SourceSelectionMode {
        USE_EXISTING,
        COPY_AS_DRAFT,
        CREATE_NEW_DRAFT
    }

    public record SourceSelection(SourceSelectionMode mode, UUID sourceId, String draftName) {
        public SourceSelection {
            if (mode == null) throw new IllegalArgumentException("Source selection mode is required");
        }
    }

    public record ContextSelection(SourceSelection ccct, SourceSelection examMatrix) {
    }

    public record DraftVersion(UUID id, long version) {
    }

    public record VariantQuestionInput(String content, RichContentFormat contentFormat,
            String explanation, RichContentFormat explanationFormat, List<AnswerInput> answers) {
        public VariantQuestionInput(String content, List<AnswerInput> answers) {
            this(content, RichContentFormat.PLAIN_TEXT, null, RichContentFormat.PLAIN_TEXT, answers);
        }
    }

    public record VariantEditingContext(AssessmentView assessment, VariantView variant, VariantQuestionView question) {
    }

    private RichTextValue sanitizeRequired(String content, RichContentFormat format, int maxLength, String message) {
        RichTextValue value = richContentPolicy.sanitize(new RichTextValue(content,
                format == null ? RichContentFormat.PLAIN_TEXT : format), maxLength);
        if (!richContentPolicy.hasMeaningfulContent(value)) throw new IllegalArgumentException(message);
        return value.contentFormat() == RichContentFormat.PLAIN_TEXT
                ? new RichTextValue(value.content().trim(), RichContentFormat.PLAIN_TEXT) : value;
    }

    private RichTextValue sanitizeOptional(String content, RichContentFormat format, int maxLength) {
        RichTextValue value = richContentPolicy.sanitize(new RichTextValue(content,
                format == null ? RichContentFormat.PLAIN_TEXT : format), maxLength);
        if (!richContentPolicy.hasMeaningfulContent(value)) return RichTextValue.plainText("");
        return value.contentFormat() == RichContentFormat.PLAIN_TEXT
                ? new RichTextValue(value.content().trim(), RichContentFormat.PLAIN_TEXT) : value;
    }

    private AnswerInput sanitizeAnswer(AnswerInput answer) {
        RichTextValue content = sanitizeRequired(answer.content(), answer.contentFormat(),
                RichContentPolicyService.MAX_ANSWER_HTML_LENGTH, "Every MCQ answer requires content");
        return new AnswerInput(content.content(), content.contentFormat(), answer.correct());
    }
}
