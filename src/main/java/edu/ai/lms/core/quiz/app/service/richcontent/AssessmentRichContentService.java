package edu.ai.lms.core.quiz.app.service.richcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.quiz.adapter.out.persistence.AssessmentWorkflowRepository.AnswerInput;
import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.service.assessment.AssessmentService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentRichContentService {
    private final AssessmentService assessments;
    private final RichContentAssetRepository assets;
    private final RichContentUploadService uploads;

    @Transactional
    public List<VariantView> updateVariantQuestion(UUID courseId, UUID assessmentId, UUID variantId, UUID questionId,
            AssessmentService.VariantQuestionInput request, List<MultipartFile> images) {
        RichContentUploadService.PreparedRichContent prepared = uploads.prepare(values(request), images,
                assets.findByQuestionId(questionId));
        uploads.cleanupAfterRollback(prepared.uploadedAssets());
        List<VariantView> result = assessments.updateVariantQuestion(courseId, assessmentId, variantId, questionId,
                rewrite(request, prepared.values()));
        assets.saveAll(uploads.assetsForQuestion(questionId, prepared.uploadedAssets()));
        return result;
    }

    private List<RichTextValue> values(AssessmentService.VariantQuestionInput request) {
        List<RichTextValue> values = new ArrayList<>();
        values.add(value(request.content(), request.contentFormat()));
        for (AnswerInput answer : request.answers() == null ? List.<AnswerInput>of() : request.answers()) {
            values.add(value(answer.content(), answer.contentFormat()));
            values.add(value(answer.explanation(), answer.explanationFormat()));
        }
        return values;
    }

    private AssessmentService.VariantQuestionInput rewrite(AssessmentService.VariantQuestionInput request, List<RichTextValue> values) {
        int[] index = {0};
        RichTextValue content = values.get(index[0]++);
        List<AnswerInput> answers = (request.answers() == null ? List.<AnswerInput>of() : request.answers()).stream()
                .map(answer -> {
                    RichTextValue answerContent = values.get(index[0]++);
                    RichTextValue explanation = values.get(index[0]++);
                    return new AnswerInput(answerContent.content(), answerContent.contentFormat(), answer.correct(),
                            explanation.content(), explanation.contentFormat());
                }).toList();
        return new AssessmentService.VariantQuestionInput(content.content(), content.contentFormat(), answers);
    }

    private RichTextValue value(String content, RichContentFormat format) {
        return new RichTextValue(content, format == null ? RichContentFormat.PLAIN_TEXT : format);
    }
}
