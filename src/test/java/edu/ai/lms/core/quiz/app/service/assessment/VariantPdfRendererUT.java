package edu.ai.lms.core.quiz.app.service.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.VariantPdfImageException;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;

class VariantPdfRendererUT {
    private RichContentAssetRepository assets;
    private RichContentAssetStoragePort storage;
    private VariantPdfRenderer renderer;

    @BeforeEach
    void setUp() {
        assets = mock(RichContentAssetRepository.class);
        storage = mock(RichContentAssetStoragePort.class);
        renderer = new VariantPdfRenderer(assets, storage);
    }

    @Test
    void rendersOnlyQuestionsAndConfiguredAnswersWithVietnameseAndMath() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(assets.findByQuestionId(questionId)).thenReturn(List.of());
        List<AnswerView> answers = List.of(
                answer("Lựa chọn A", true, 0), answer("Lựa chọn B", false, 1), answer("Lựa chọn C", false, 2),
                answer("Lựa chọn D", false, 3), answer("Lựa chọn E", false, 4));
        String richQuestion = "<p>Tính <span data-type=\"inline-math\" data-latex=\"\\frac{x^2}{y_1}\"></span></p>"
                + "<table><tbody><tr><td>Ô tiếng Việt</td></tr></tbody></table>";
        VariantView variant = variant(questionId, richQuestion, RichContentFormat.TIPTAP_HTML, answers);

        byte[] pdf = renderer.render(variant);
        String text = text(pdf);

        assertTrue(pdf.length > 1_000);
        assertTrue(text.contains("Câu 1:"));
        assertTrue(text.contains("Ô tiếng Việt"));
        assertTrue(text.contains("A."));
        assertTrue(text.contains("E."));
        assertFalse(text.contains("Giải thích bí mật"));
        assertFalse(text.contains("Matrix Row nội bộ"));
        assertFalse(text.contains("Đúng"));
    }

    @Test
    void keepsUnsupportedLatexAsRawText() throws Exception {
        UUID questionId = UUID.randomUUID();
        when(assets.findByQuestionId(questionId)).thenReturn(List.of());
        String unsupported = "\\commandThatDoesNotExist{abc}";
        VariantView variant = variant(questionId,
                "<p><span data-type=\"inline-math\" data-latex=\"" + unsupported + "\"></span></p>",
                RichContentFormat.TIPTAP_HTML, fourAnswers());

        String text = text(renderer.render(variant));

        assertTrue(text.contains("commandThatDoesNotExist"));
        assertTrue(text.contains("abc"));
    }

    @Test
    void embedsAnOwnedImageAfterCheckingItsMetadata() throws Exception {
        UUID questionId = UUID.randomUUID();
        byte[] png = png();
        RichContentAsset asset = asset(questionId, "quiz2/rich-content/diagram.png", "image/png", png);
        when(assets.findByQuestionId(questionId)).thenReturn(List.of(asset));
        when(storage.publicUrl(asset.storageKey())).thenReturn("https://assets.example/diagram.png");
        when(storage.read(asset.storageKey())).thenReturn(png);
        VariantView variant = variant(questionId,
                "<p>Sơ đồ</p><img src=\"https://assets.example/diagram.png\" alt=\"Sơ đồ lực\">",
                RichContentFormat.TIPTAP_HTML, fourAnswers());

        byte[] pdf = renderer.render(variant);

        assertTrue(pdf.length > 1_000);
        try (var document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
            assertTrue(document.getPage(0).getResources().getXObjectNames().iterator().hasNext());
        }
    }

    @Test
    void reportsTheExactAnswerAndImageWhenAnAssetIsBroken() throws Exception {
        UUID questionId = UUID.randomUUID();
        byte[] png = png();
        RichContentAsset asset = asset(questionId, "quiz2/rich-content/broken.png", "image/png", png);
        when(assets.findByQuestionId(questionId)).thenReturn(List.of(asset));
        when(storage.publicUrl(asset.storageKey())).thenReturn("https://assets.example/broken.png");
        when(storage.read(asset.storageKey())).thenReturn(new byte[] {1, 2, 3});
        List<AnswerView> answers = List.of(
                answer("A", true, 0),
                new AnswerView(UUID.randomUUID(),
                        "<img src=\"https://assets.example/broken.png\" alt=\"Hình lực\">",
                        RichContentFormat.TIPTAP_HTML, false, "Giải thích bí mật", RichContentFormat.PLAIN_TEXT, 1),
                answer("C", false, 2), answer("D", false, 3));
        VariantView variant = variant(questionId, "Đề bài", RichContentFormat.PLAIN_TEXT, answers);

        VariantPdfImageException error = assertThrows(VariantPdfImageException.class,
                () -> renderer.render(variant));

        assertEquals("questions[0].answers[1].images[0]", error.getFieldPath());
        assertTrue(error.getMessage().contains("Câu 1, đáp án B, ảnh 1"));
        assertTrue(error.getDetails().contains("storageKey=" + asset.storageKey()));
    }

    @Test
    void supportsGifWebpAndSvgAssets() throws Exception {
        UUID questionId = UUID.randomUUID();
        byte[] gif = gif();
        byte[] webp = Base64.getDecoder().decode("UklGRhwAAABXRUJQVlA4TA8AAAAvA4AAAAcQ/Y/+ByKi/wEA");
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"60\" height=\"30\"><rect width=\"60\" height=\"30\" fill=\"green\"/></svg>"
                .getBytes(StandardCharsets.UTF_8);
        List<RichContentAsset> owned = List.of(
                asset(questionId, "quiz2/rich-content/a.gif", "image/gif", gif),
                asset(questionId, "quiz2/rich-content/b.webp", "image/webp", webp),
                asset(questionId, "quiz2/rich-content/c.svg", "image/svg+xml", svg));
        when(assets.findByQuestionId(questionId)).thenReturn(owned);
        for (RichContentAsset asset : owned) {
            String url = "https://assets.example/" + asset.storageKey().substring(asset.storageKey().lastIndexOf('/') + 1);
            when(storage.publicUrl(asset.storageKey())).thenReturn(url);
            when(storage.read(asset.storageKey())).thenReturn(
                    asset.mediaType().equals("image/gif") ? gif : asset.mediaType().equals("image/webp") ? webp : svg);
        }
        VariantView variant = variant(questionId,
                "<img src=\"https://assets.example/a.gif\" alt=\"GIF\">"
                + "<img src=\"https://assets.example/b.webp\" alt=\"WebP\">"
                + "<img src=\"https://assets.example/c.svg\" alt=\"SVG\">",
                RichContentFormat.TIPTAP_HTML, fourAnswers());

        byte[] pdf = renderer.render(variant);

        assertTrue(pdf.length > 1_000);
        try (var document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void writesAVisualUatArtifactWhenRequested() throws Exception {
        String output = System.getProperty("variant.pdf.uat.output");
        Assumptions.assumeTrue(output != null && !output.isBlank());
        UUID questionId = UUID.randomUUID();
        byte[] png = png();
        RichContentAsset asset = asset(questionId, "quiz2/rich-content/uat-diagram.png", "image/png", png);
        when(assets.findByQuestionId(questionId)).thenReturn(List.of(asset));
        when(storage.publicUrl(asset.storageKey())).thenReturn("https://assets.example/uat-diagram.png");
        when(storage.read(asset.storageKey())).thenReturn(png);
        String content = "<p>Cho hệ thức <span data-type=\"inline-math\" data-latex=\"E=mc^2\"></span>. Tính biểu thức:</p>"
                + "<div data-type=\"block-math\" data-latex=\"\\frac{x+1}{x-1}+\\sqrt{a^2+b^2}+\\int_0^1 x^2\\,dx\"></div>"
                + "<div data-type=\"block-math\" data-latex=\"\\begin{pmatrix}1 &amp; 2 \\\\ 3 &amp; 4\\end{pmatrix}\"></div>"
                + "<p>Quan sát hình:</p><img src=\"https://assets.example/uat-diagram.png\" alt=\"Sơ đồ UAT\">";
        VariantView variant = variant(questionId, content, RichContentFormat.TIPTAP_HTML, List.of(
                answer("Đáp án thứ nhất với tiếng Việt", true, 0),
                answer("Đáp án thứ hai", false, 1), answer("Đáp án thứ ba", false, 2),
                answer("Đáp án thứ tư", false, 3), answer("Đáp án thứ năm", false, 4)));

        byte[] pdf = renderer.render(variant);
        Path path = Path.of(output).toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Files.write(path, pdf);
        assertTrue(Files.size(path) > 1_000);
    }

    private VariantView variant(UUID questionId, String content, RichContentFormat format, List<AnswerView> answers) {
        VariantQuestionView question = new VariantQuestionView(questionId, UUID.randomUUID(), content, format,
                "MULTIPLE_CHOICE", null, answers, 0);
        return new VariantView(UUID.randomUUID(), UUID.randomUUID(), "001", 0, 1L, "COMPLIANT", null, null, null,
                List.of(question));
    }

    private List<AnswerView> fourAnswers() {
        return List.of(answer("A", true, 0), answer("B", false, 1), answer("C", false, 2), answer("D", false, 3));
    }

    private AnswerView answer(String content, boolean correct, int order) {
        return new AnswerView(UUID.randomUUID(), content, RichContentFormat.PLAIN_TEXT, correct,
                "Giải thích bí mật", RichContentFormat.PLAIN_TEXT, order);
    }

    private RichContentAsset asset(UUID questionId, String key, String mediaType, byte[] bytes) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return new RichContentAsset(UUID.randomUUID(), null, null, questionId, key, mediaType, bytes.length, hash,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 40, 30);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private byte[] gif() throws Exception {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "gif", output);
            return output.toByteArray();
        }
    }

    private String text(byte[] pdf) throws Exception {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
