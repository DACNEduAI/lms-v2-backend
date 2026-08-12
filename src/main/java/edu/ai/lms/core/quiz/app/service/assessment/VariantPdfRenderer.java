package edu.ai.lms.core.quiz.app.service.assessment;

import static com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase.DOCUMENT;
import static com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase.MATHML;
import static com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase.SVG;
import static com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;

import com.openhtmltopdf.latexsupport.LaTeXDOMMutator;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.VariantPdfImageException;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;
import lombok.RequiredArgsConstructor;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;

@Service
@RequiredArgsConstructor
public class VariantPdfRenderer {
    private static final String REGULAR_FONT = "/fonts/NotoSans-Regular.ttf";
    private static final String BOLD_FONT = "/fonts/NotoSans-Bold.ttf";
    private static final Set<com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase> FONT_USES =
            Set.of(DOCUMENT, SVG, MATHML);

    private static final String CSS = """
            @page { size: A4 portrait; margin: 14mm; }
            html, body { margin: 0; padding: 0; background: #fff; color: #111; }
            body { font-family: 'Noto Sans'; font-size: 11pt; line-height: 1.5; }
            .question { margin: 0 0 7mm 0; page-break-inside: avoid; }
            .row { display: table; width: 100%; table-layout: fixed; }
            .label, .content { display: table-cell; vertical-align: top; }
            .label { width: 15mm; font-weight: bold; white-space: nowrap; }
            .answers { margin: 3mm 0 0 5mm; padding: 0; }
            .answer { margin: 0 0 2.5mm 0; page-break-inside: avoid; }
            .content > :first-child { margin-top: 0; }
            .content > :last-child { margin-bottom: 0; }
            p { margin: 0 0 2mm 0; }
            ul, ol { margin-top: 1mm; margin-bottom: 2mm; }
            table { width: 100%; border-collapse: collapse; page-break-inside: auto; }
            th, td { border: 0.25mm solid #777; padding: 1.5mm; vertical-align: top; }
            tr, pre, blockquote { page-break-inside: avoid; }
            img, svg { max-width: 100%; height: auto; page-break-inside: avoid; }
            .math-block { display: block; width: 100%; margin: 1.5mm 0; text-align: center; }
            .latex-fallback { font-family: monospace; word-wrap: break-word; white-space: pre-wrap; }
            """;

    private final RichContentAssetRepository assets;
    private final RichContentAssetStoragePort storage;

    public byte[] render(VariantView variant) {
        Document document = Document.createShell("");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml).prettyPrint(false).charset(StandardCharsets.UTF_8);
        document.selectFirst("html").attr("xmlns", "http://www.w3.org/1999/xhtml");
        document.head().appendElement("meta").attr("charset", "UTF-8");
        document.head().appendElement("style").attr("type", "text/css").appendText(CSS);

        Map<String, PreparedImage> imageCache = new HashMap<>();
        for (int questionIndex = 0; questionIndex < variant.questions().size(); questionIndex++) {
            VariantQuestionView question = variant.questions().get(questionIndex);
            Map<String, RichContentAsset> ownedAssets = ownedAssets(question.id());
            Element questionElement = document.body().appendElement("div").addClass("question");
            appendRow(questionElement, "Câu " + (questionIndex + 1) + ":",
                    question.content(), question.contentFormat(), new ContentLocation(variant.id(), question.id(),
                            questionIndex, null), ownedAssets, imageCache);
            Element answersElement = questionElement.appendElement("div").addClass("answers");
            for (int answerIndex = 0; answerIndex < question.answers().size(); answerIndex++) {
                AnswerView answer = question.answers().get(answerIndex);
                String label = String.valueOf((char) ('A' + answerIndex));
                Element answerElement = answersElement.appendElement("div").addClass("answer");
                appendRow(answerElement, label + ".", answer.content(), answer.contentFormat(),
                        new ContentLocation(variant.id(), question.id(), questionIndex, answerIndex), ownedAssets,
                        imageCache);
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(document.outerHtml(), null);
            builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
            builder.useMathMLDrawer(new MathMLDrawer());
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.useFont(() -> font(REGULAR_FONT), "Noto Sans", 400, NORMAL, true, FONT_USES);
            builder.useFont(() -> font(BOLD_FONT), "Noto Sans", 700, NORMAL, true, FONT_USES);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not render variant PDF", ex);
        }
    }

    private void appendRow(Element parent, String label, String content, RichContentFormat format,
            ContentLocation location, Map<String, RichContentAsset> ownedAssets,
            Map<String, PreparedImage> imageCache) {
        Element row = parent.appendElement("div").addClass("row");
        row.appendElement("div").addClass("label").text(label);
        Element value = row.appendElement("div").addClass("content");
        if (format == null || format == RichContentFormat.PLAIN_TEXT) {
            appendPlainText(value, content);
            return;
        }

        Document fragment = Jsoup.parseBodyFragment(content == null ? "" : content);
        prepareMath(fragment.body());
        prepareImages(fragment.body(), location, ownedAssets, imageCache);
        for (Node child : new ArrayList<>(fragment.body().childNodes())) {
            value.appendChild(child.clone());
        }
    }

    private void appendPlainText(Element target, String content) {
        String[] lines = (content == null ? "" : content).split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) target.appendElement("br");
            target.appendText(lines[index]);
        }
    }

    private void prepareMath(Element root) {
        for (Element math : new ArrayList<>(root.select("[data-latex]"))) {
            String latex = math.attr("data-latex");
            boolean block = "block-math".equals(math.attr("data-type"));
            String wrapped = block ? "\\[" + latex + "\\]" : "$" + latex + "$";
            if (!canRenderLatex(wrapped)) {
                Element fallback = new Element(block ? "div" : "code").addClass("latex-fallback").text(latex);
                if (block) fallback.addClass("math-block");
                math.replaceWith(fallback);
                continue;
            }
            Element latexElement = new Element("latex").text(wrapped);
            if (block) {
                Element wrapper = new Element("div").addClass("math-block");
                wrapper.appendChild(latexElement);
                math.replaceWith(wrapper);
            } else {
                math.replaceWith(latexElement);
            }
        }
    }

    private boolean canRenderLatex(String latex) {
        try {
            SnuggleSession session = new SnuggleEngine().createSession();
            boolean parsed = session.parseInput(new SnuggleInput(latex, "Quiz PDF formula"));
            return parsed && session.getErrors().isEmpty();
        } catch (RuntimeException | IOException ex) {
            return false;
        }
    }

    private Map<String, RichContentAsset> ownedAssets(UUID questionId) {
        Map<String, RichContentAsset> result = new LinkedHashMap<>();
        for (RichContentAsset asset : assets.findByQuestionId(questionId)) {
            result.put(storage.publicUrl(asset.storageKey()), asset);
        }
        return result;
    }

    private void prepareImages(Element root, ContentLocation location, Map<String, RichContentAsset> ownedAssets,
            Map<String, PreparedImage> imageCache) {
        List<Element> images = new ArrayList<>(root.select("img"));
        for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
            Element image = images.get(imageIndex);
            String source = image.attr("src");
            String alt = image.attr("alt");
            RichContentAsset asset = ownedAssets.get(source);
            if (asset == null) {
                throw imageFailure(location, imageIndex, alt, "unowned", "Ảnh không thuộc nội dung câu hỏi", null);
            }
            PreparedImage prepared;
            try {
                prepared = imageCache.computeIfAbsent(asset.storageKey(), ignored -> prepareAsset(asset));
            } catch (ImagePreparationException ex) {
                throw imageFailure(location, imageIndex, alt, asset.storageKey(), ex.getMessage(), ex);
            }
            if (prepared.svg() != null) {
                Document svgDocument = Jsoup.parse(prepared.svg(), "", Parser.xmlParser());
                Element svg = svgDocument.selectFirst("svg");
                if (svg == null) {
                    throw imageFailure(location, imageIndex, alt, asset.storageKey(), "SVG không hợp lệ", null);
                }
                svg.addClass("embedded-image");
                image.replaceWith(svg.clone());
            } else {
                image.attr("src", "data:" + prepared.mediaType() + ";base64,"
                        + Base64.getEncoder().encodeToString(prepared.bytes()));
            }
        }
    }

    private PreparedImage prepareAsset(RichContentAsset asset) {
        byte[] bytes;
        try {
            bytes = storage.read(asset.storageKey());
        } catch (RuntimeException ex) {
            throw new ImagePreparationException("Không đọc được object từ storage", ex);
        }
        if (bytes.length != asset.byteSize()) {
            throw new ImagePreparationException("Kích thước ảnh không khớp metadata");
        }
        if (!sha256(bytes).equalsIgnoreCase(asset.sha256())) {
            throw new ImagePreparationException("Checksum ảnh không khớp metadata");
        }
        String detected = detectMediaType(bytes);
        if (!detected.equalsIgnoreCase(asset.mediaType())) {
            throw new ImagePreparationException("MIME ảnh không khớp chữ ký file");
        }
        if ("image/svg+xml".equals(detected)) {
            String svg = new String(bytes, StandardCharsets.UTF_8).trim();
            String lower = svg.toLowerCase(Locale.ROOT);
            if (lower.contains("<!doctype") || lower.contains("<!entity") || lower.contains("<script")
                    || lower.contains("<foreignobject") || lower.contains("javascript:")) {
                throw new ImagePreparationException("SVG chứa nội dung không an toàn");
            }
            return new PreparedImage(detected, null, svg);
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException ex) {
            throw new ImagePreparationException("Không giải mã được ảnh", ex);
        }
        if (decoded == null) throw new ImagePreparationException("Không giải mã được ảnh");
        if ("image/png".equals(detected) || "image/jpeg".equals(detected)) {
            return new PreparedImage(detected, bytes, null);
        }
        try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
            if (!ImageIO.write(decoded, "png", png)) throw new ImagePreparationException("Không chuyển được ảnh sang PNG");
            return new PreparedImage("image/png", png.toByteArray(), null);
        } catch (IOException ex) {
            throw new ImagePreparationException("Không chuyển được ảnh sang PNG", ex);
        }
    }

    private String detectMediaType(byte[] bytes) {
        if (startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return "image/png";
        if (startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) return "image/jpeg";
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) return "image/gif";
        if (startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII)) && bytes.length > 12
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) return "image/webp";
        String text = new String(bytes, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("<svg") || text.startsWith("<?xml") && text.contains("<svg")) return "image/svg+xml";
        throw new ImagePreparationException("Loại ảnh không được hỗ trợ");
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (source[index] != prefix[index]) return false;
        return true;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private VariantPdfImageException imageFailure(ContentLocation location, int imageIndex, String alt,
            String storageKey, String reason, Throwable cause) {
        String answer = location.answerIndex() == null ? "" : ", đáp án " + (char) ('A' + location.answerIndex());
        String safeAlt = alt == null || alt.isBlank() ? "không có alt" : alt;
        String message = "Không thể tải ảnh tại Câu " + (location.questionIndex() + 1) + answer + ", ảnh "
                + (imageIndex + 1) + " (" + safeAlt + ").";
        String fieldPath = "questions[" + location.questionIndex() + "]"
                + (location.answerIndex() == null ? ".content" : ".answers[" + location.answerIndex() + "]")
                + ".images[" + imageIndex + "]";
        return new VariantPdfImageException(message, fieldPath, location.variantId(), location.questionId(),
                List.of("alt=" + safeAlt, "storageKey=" + storageKey, "reason=" + reason), cause);
    }

    private InputStream font(String path) {
        InputStream stream = VariantPdfRenderer.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Bundled PDF font is missing: " + path);
        return stream;
    }

    private record ContentLocation(UUID variantId, UUID questionId, int questionIndex, Integer answerIndex) { }
    private record PreparedImage(String mediaType, byte[] bytes, String svg) { }

    private static final class ImagePreparationException extends RuntimeException {
        private ImagePreparationException(String message) { super(message); }
        private ImagePreparationException(String message, Throwable cause) { super(message, cause); }
    }
}
