package edu.ai.lms.core.quiz.app.service.richcontent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.quiz.adapter.in.http.request.QuestionBankItemRequest;
import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionDraft;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.port.in.usecase.QuestionBankUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionBankRichContentService {
    private final QuestionBankUseCase questionBank;
    private final RichContentAssetRepository assets;
    private final RichContentUploadService uploads;

    @Transactional
    public QuestionDraft createDraft(UUID courseId, QuestionBankItemRequest request, List<MultipartFile> images) {
        RichContentUploadService.PreparedRichContent prepared = uploads.prepare(values(request), images, List.of());
        uploads.cleanupAfterRollback(prepared.uploadedAssets());
        QuestionDraft saved = questionBank.createDraft(courseId, rewrite(request, prepared.values()).toCommand());
        assets.saveAll(uploads.assetsForDraft(saved.getId(), prepared.uploadedAssets()));
        return saved;
    }

    @Transactional
    public QuestionDraft updateDraft(UUID courseId, UUID draftId, long version, QuestionBankItemRequest request,
            List<MultipartFile> images) {
        RichContentUploadService.PreparedRichContent prepared = uploads.prepare(values(request), images,
                assets.findByQuestionDraftId(draftId));
        uploads.cleanupAfterRollback(prepared.uploadedAssets());
        QuestionDraft saved = questionBank.updateDraft(courseId, draftId, version,
                rewrite(request, prepared.values()).toCommand());
        assets.saveAll(uploads.assetsForDraft(saved.getId(), prepared.uploadedAssets()));
        return saved;
    }

    private List<RichTextValue> values(QuestionBankItemRequest request) {
        List<RichTextValue> values = new ArrayList<>();
        values.add(value(request.content(), request.contentFormat()));
        for (QuestionBankItemRequest.AnswerRequest answer : request.answers() == null ? List.<QuestionBankItemRequest.AnswerRequest>of() : request.answers()) {
            values.add(value(answer.content(), answer.contentFormat()));
            values.add(value(answer.explanation(), answer.explanationFormat()));
        }
        return values;
    }

    private QuestionBankItemRequest rewrite(QuestionBankItemRequest request, List<RichTextValue> values) {
        int[] index = {0};
        RichTextValue content = values.get(index[0]++);
        List<QuestionBankItemRequest.AnswerRequest> answers = (request.answers() == null ? List.<QuestionBankItemRequest.AnswerRequest>of() : request.answers()).stream()
                .map(answer -> {
                    RichTextValue answerContent = values.get(index[0]++);
                    RichTextValue explanation = values.get(index[0]++);
                    return new QuestionBankItemRequest.AnswerRequest(answerContent.content(), answerContent.contentFormat(),
                            answer.correct(), explanation.content(), explanation.contentFormat());
                }).toList();
        return new QuestionBankItemRequest(request.chapterId(), content.content(), content.contentFormat(), request.questionType(),
                request.cognitiveLevelCode(), answers, request.learningOutcomeIds());
    }

    private RichTextValue value(String content, RichContentFormat format) {
        return new RichTextValue(content, format == null ? RichContentFormat.PLAIN_TEXT : format);
    }
}
