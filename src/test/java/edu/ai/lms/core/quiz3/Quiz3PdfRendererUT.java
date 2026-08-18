package edu.ai.lms.core.quiz3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.VariantPdfImageException;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;

class Quiz3PdfRendererUT {
    @Test
    void buildsHtmlUsingQuiz3OwnedAssetsOnly() {
        UUID questionId = UUID.randomUUID();
        Quiz3RichContentAssetRepository assets = mock(Quiz3RichContentAssetRepository.class);
        when(assets.findByQuestionId(questionId)).thenReturn(List.of());
        String html = new Quiz3PdfRenderer(assets, mock(RichContentAssetStoragePort.class)).buildHtml(variant(questionId, "<p>Nội dung</p>"));
        assertThat(html).contains("Nội dung").contains("Câu 1:");
    }

    @Test
    void rejectsImageWithoutQuiz3Metadata() {
        UUID questionId = UUID.randomUUID();
        Quiz3RichContentAssetRepository assets = mock(Quiz3RichContentAssetRepository.class);
        when(assets.findByQuestionId(questionId)).thenReturn(List.of());
        Quiz3PdfRenderer renderer = new Quiz3PdfRenderer(assets, mock(RichContentAssetStoragePort.class));
        assertThatThrownBy(() -> renderer.buildHtml(variant(questionId, "<img src=\"https://other/diagram.svg\" alt=\"x\">")))
                .isInstanceOf(VariantPdfImageException.class)
                .hasMessageContaining("Câu 1");
    }

    private VariantView variant(UUID questionId, String content) {
        VariantQuestionView question = new VariantQuestionView(questionId, UUID.randomUUID(), content,
                RichContentFormat.TIPTAP_HTML, "MULTIPLE_CHOICE", null,
                List.of(new AnswerView(UUID.randomUUID(), "A", RichContentFormat.PLAIN_TEXT, true, "", RichContentFormat.PLAIN_TEXT, 0)), 0);
        return new VariantView(UUID.randomUUID(), UUID.randomUUID(), "001", 0, 1L, "COMPLIANT", null, null, null, List.of(question));
    }
}
