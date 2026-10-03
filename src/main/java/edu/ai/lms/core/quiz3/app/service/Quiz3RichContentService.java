package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichTextValue;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Quiz3RichContentService {
    private final Quiz3AssessmentService assessments;
    private final Quiz3RichContentAssetRepository assets;
    private final Quiz3RichContentUploadService uploads;
    private final Quiz3RichContentNormalizer normalizer;

    @Transactional
    public List<Quiz3Models.VariantView> update(UUID courseId, UUID assessmentId, UUID variantId,
            UUID questionId, Quiz3Models.QuestionUpdate request, List<MultipartFile> images) {
        var existing = assets.findByQuestionId(questionId);
        var prepared = uploads.prepare(values(request), images, existing);
        uploads.cleanupAfterRollback(prepared.uploadedAssets());
        var allowedImageUrls = uploads.publicUrls(existing, prepared.uploadedAssets());
        var normalizedValues = prepared.values().stream()
                .map(value -> normalizer.normalizeDeclared(value.content(), value.contentFormat().name(), allowedImageUrls::contains))
                .toList();
        int index = 0;
        RichTextValue content = normalizedValues.get(index++);
        RichTextValue explanation = normalizedValues.get(index++);
        List<Quiz3Models.AnswerInput> answers = new ArrayList<>();
        List<Quiz3Models.AnswerInput> requestedAnswers = request.answers() == null ? List.of() : request.answers();
        for (Quiz3Models.AnswerInput answer : requestedAnswers) {
            RichTextValue value = normalizedValues.get(index++);
            answers.add(new Quiz3Models.AnswerInput(value.content(), value.contentFormat().name(), answer.correct()));
        }
        String explanationContent = request.explanation() == null ? null : explanation.content();
        var rewritten = new Quiz3Models.QuestionUpdate(request.expectedQuestionVersion(), content.content(),
                content.contentFormat().name(), explanationContent, explanation.contentFormat().name(), answers,
                request.imageAssetSelections());
        List<Quiz3Models.VariantView> result = assessments.updateQuestion(
                courseId, assessmentId, variantId, questionId, rewritten);
        assets.saveAll(uploads.assetsForQuestion(questionId, prepared.uploadedAssets()));
        assets.retainForQuestion(questionId, uploads.referencedStorageKeys(
                normalizedValues, existing, prepared.uploadedAssets()));
        return result;
    }

    private List<RichTextValue> values(Quiz3Models.QuestionUpdate request) {
        List<RichTextValue> values = new ArrayList<>();
        values.add(value(request.content(), request.contentFormat()));
        values.add(value(request.explanation(), request.explanationFormat()));
        for (Quiz3Models.AnswerInput answer : request.answers() == null ? List.<Quiz3Models.AnswerInput>of() : request.answers()) {
            values.add(value(answer.content(), answer.contentFormat()));
        }
        return values;
    }

    private RichTextValue value(String content, String format) {
        return new RichTextValue(content == null ? "" : content,
                "TIPTAP_HTML".equals(format) ? RichContentFormat.TIPTAP_HTML : RichContentFormat.PLAIN_TEXT);
    }
}
