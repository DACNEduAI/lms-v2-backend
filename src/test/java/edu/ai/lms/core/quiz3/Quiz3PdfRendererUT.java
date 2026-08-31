package edu.ai.lms.core.quiz3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.VariantPdfImageException;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;
import edu.ai.lms.core.quiz.api.CodeRunnerContractSnapshotApi.PrintableContract;
import edu.ai.lms.core.quiz.api.CodeRunnerContractSnapshotApi.PublicExample;

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

    @Test
    void buildsProfessionalCodeRunnerExamWithSafeContractDataAndWritingSpace() throws Exception {
        UUID questionId = UUID.randomUUID();
        Quiz3RichContentAssetRepository assets = mock(Quiz3RichContentAssetRepository.class);
        when(assets.findByQuestionId(questionId)).thenReturn(List.of());
        Quiz3PdfRenderer renderer = new Quiz3PdfRenderer(assets, mock(RichContentAssetStoragePort.class));
        var variant = codeRunnerVariant(questionId, "Viết hàm sau:\n```cpp\nold starter code\n```");
        var contract = new PrintableContract("Contract statement", "sortValues",
                "std::vector<int> sortValues(std::vector<int> values) {\n    // TODO\n}",
                List.of(new PublicExample(
                        Map.<String, JsonNode>of("values", JsonNodeFactory.instance.arrayNode().add(3).add(1)),
                        JsonNodeFactory.instance.arrayNode().add(1).add(3))),
                5);
        var document = new Quiz3PdfRenderer.PrintDocument("Thi cuối kỳ", "Không sử dụng tài liệu", "CO2003",
                "Cấu trúc dữ liệu và giải thuật", "001", 60, Quiz3PdfRenderer.AnswerMode.WRITE_ON_EXAM,
                variant, List.of(new Quiz3PdfRenderer.PrintQuestion(variant.questions().getFirst(), contract)));

        String html = renderer.buildHtml(document);

        assertThat(html).contains("Đề thi lập trình", "Thi cuối kỳ", "Mã đề:", "001", "60 phút",
                "Họ và tên:", "MSSV:", "sortValues", "std::vector&lt;int&gt; sortValues", "Ví dụ công khai",
                "Phần bài làm của thí sinh", "5 điểm");
        assertThat(html).doesNotContain("```cpp", "old starter code");

        byte[] pdf = renderer.render(document);
        assertThat(pdf).hasSizeGreaterThan(1_000).startsWith('%', 'P', 'D', 'F');
        String output = System.getProperty("quiz3.pdf.output");
        if (output != null && !output.isBlank()) Files.write(Path.of(output), pdf);
    }

    private VariantView variant(UUID questionId, String content) {
        VariantQuestionView question = new VariantQuestionView(questionId, UUID.randomUUID(), content,
                RichContentFormat.TIPTAP_HTML, "MULTIPLE_CHOICE", null,
                List.of(new AnswerView(UUID.randomUUID(), "A", RichContentFormat.PLAIN_TEXT, true, "", RichContentFormat.PLAIN_TEXT, 0)), 0);
        return new VariantView(UUID.randomUUID(), UUID.randomUUID(), "001", 0, 1L, "COMPLIANT", null, null, null, List.of(question));
    }

    private VariantView codeRunnerVariant(UUID questionId, String content) {
        VariantQuestionView question = new VariantQuestionView(questionId, UUID.randomUUID(), content,
                RichContentFormat.PLAIN_TEXT, "CODE_RUNNER", null, List.of(), 0);
        return new VariantView(UUID.randomUUID(), UUID.randomUUID(), "001", 0, 1L, "COMPLIANT", null, null, null, List.of(question));
    }
}
