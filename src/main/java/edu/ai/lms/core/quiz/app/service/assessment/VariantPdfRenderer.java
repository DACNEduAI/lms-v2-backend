package edu.ai.lms.core.quiz.app.service.assessment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;

import edu.ai.lms.core.quiz.adapter.out.persistence.RichContentAssetRepository;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentAsset;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.VariantPdfFormulaException;
import edu.ai.lms.core.quiz.app.exception.VariantPdfImageException;
import edu.ai.lms.core.quiz.app.exception.VariantPdfRenderUnavailableException;
import edu.ai.lms.core.quiz.app.port.out.RichContentAssetStoragePort;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/**
 * Browser-backed PDF renderer. It deliberately uses the same KaTeX HTML/CSS
 * model as the teacher editor instead of translating LaTeX through SnuggleTeX.
 */
@Service
@RequiredArgsConstructor
public class VariantPdfRenderer {
    private static final String KATEX_VERSION = "0.16.22";
    private static final String KATEX_BASE = "/META-INF/resources/webjars/katex/" + KATEX_VERSION + "/dist/";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern KATEX_FONT_URL = Pattern.compile("url\\((?:['\\\"])?fonts/([^)'\\\"]+)(?:['\\\"])?\\)");
    private static final int IMAGE_MIN_WIDTH = 10;
    private static final int IMAGE_MAX_WIDTH = 100;

    private static final String CSS = """
            @page { size: A4 portrait; margin: 14mm; }
            html, body { margin: 0; padding: 0; background: #fff; color: #111; }
            body { font-family: 'Noto Sans', sans-serif; font-size: 11pt; line-height: 1.5; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
            .question { margin: 0 0 7mm 0; break-inside: avoid; page-break-inside: avoid; }
            .row { display: table; width: 100%; table-layout: fixed; }
            .label, .content { display: table-cell; vertical-align: top; }
            .label { width: 15mm; font-weight: bold; white-space: nowrap; }
            .answers { margin: 3mm 0 0 5mm; padding: 0; }
            .answer { margin: 0 0 2.5mm 0; break-inside: avoid; page-break-inside: avoid; }
            .content > :first-child { margin-top: 0; }
            .content > :last-child { margin-bottom: 0; }
            p { margin: 0 0 2mm 0; }
            ul, ol { margin-top: 1mm; margin-bottom: 2mm; }
            table { width: 100%; border-collapse: collapse; break-inside: auto; page-break-inside: auto; }
            th, td { border: .25mm solid #777; padding: 1.5mm; vertical-align: top; }
            tr, pre, blockquote { break-inside: avoid; page-break-inside: avoid; }
            img { display: inline-block; height: auto; max-width: 100%; margin: .15rem .25rem; vertical-align: middle; break-inside: avoid; page-break-inside: avoid; }
            .math-block { display: block; width: 100%; margin: 1.5mm 0; text-align: center; }
            .katex-display { margin: 0; }
            """;

    private final RichContentAssetRepository assets;
    private final RichContentAssetStoragePort storage;
    private final Semaphore renderSlots = new Semaphore(1);

    @Value("${quiz.pdf.render.timeout-seconds:30}")
    private long renderTimeoutSeconds;

    @Value("${quiz.pdf.chromium.executable:}")
    private String chromiumExecutable;

    private volatile Playwright playwright;
    private volatile Browser browser;

    public byte[] render(VariantView variant) {
        boolean acquired = false;
        try {
            acquired = renderSlots.tryAcquire(renderTimeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) throw unavailable("PDF renderer is busy. Please try again.", null);
            String html = buildHtml(variant);
            try (BrowserContext context = browser().newContext()) {
                context.setDefaultTimeout(Duration.ofSeconds(renderTimeoutSeconds).toMillis());
                context.route("**/*", route -> route.abort());
                Page page = context.newPage();
                page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.addScriptTag(new Page.AddScriptTagOptions().setContent(katexJavascript()));
                ensureRendererReady(page);
                ensureFormulasRendered(page, variant.id());
                ensureKatexFontsReady(page);
                return page.pdf(new Page.PdfOptions().setFormat("A4").setPrintBackground(true)
                        .setPreferCSSPageSize(true).setMargin(new Margin()
                                .setTop("0").setRight("0").setBottom("0").setLeft("0")));
            }
        } catch (VariantPdfFormulaException | VariantPdfImageException | VariantPdfRenderUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable("PDF rendering was interrupted", ex);
        } catch (PlaywrightException | IOException ex) {
            closeBrowser();
            throw unavailable("Chromium could not render this PDF", ex);
        } catch (RuntimeException ex) {
            throw unavailable("Chromium could not render this PDF", ex);
        } finally {
            if (acquired) renderSlots.release();
        }
    }

    String buildHtml(VariantView variant) {
        Document document = Document.createShell("");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml).prettyPrint(false).charset(StandardCharsets.UTF_8);
        document.selectFirst("html").attr("xmlns", "http://www.w3.org/1999/xhtml");
        document.head().appendElement("meta").attr("charset", "UTF-8");
        document.head().appendElement("style").attr("type", "text/css")
                .appendChild(new DataNode(CSS + "\n" + katexStylesheet()));

        Map<String, PreparedImage> imageCache = new HashMap<>();
        for (int questionIndex = 0; questionIndex < variant.questions().size(); questionIndex++) {
            VariantQuestionView question = variant.questions().get(questionIndex);
            Map<String, RichContentAsset> ownedAssets = ownedAssets(question.id());
            Element questionElement = document.body().appendElement("div").addClass("question");
            appendRow(questionElement, "Câu " + (questionIndex + 1) + ":", question.content(), question.contentFormat(),
                    new ContentLocation(variant.id(), question.id(), questionIndex, null), ownedAssets, imageCache);
            Element answersElement = questionElement.appendElement("div").addClass("answers");
            for (int answerIndex = 0; answerIndex < question.answers().size(); answerIndex++) {
                AnswerView answer = question.answers().get(answerIndex);
                Element answerElement = answersElement.appendElement("div").addClass("answer");
                appendRow(answerElement, String.valueOf((char) ('A' + answerIndex)) + ".", answer.content(),
                        answer.contentFormat(), new ContentLocation(variant.id(), question.id(), questionIndex, answerIndex),
                        ownedAssets, imageCache);
            }
        }
        return "<!doctype html>\n" + document.outerHtml();
    }

    private void appendRow(Element parent, String label, String content, RichContentFormat format,
            ContentLocation location, Map<String, RichContentAsset> ownedAssets, Map<String, PreparedImage> imageCache) {
        Element row = parent.appendElement("div").addClass("row");
        row.appendElement("div").addClass("label").text(label);
        Element value = row.appendElement("div").addClass("content");
        if (format == null || format == RichContentFormat.PLAIN_TEXT) {
            appendPlainText(value, content);
            return;
        }
        Document fragment = Jsoup.parseBodyFragment(content == null ? "" : content);
        prepareMath(fragment.body(), location);
        prepareImages(fragment.body(), location, ownedAssets, imageCache);
        for (Node child : new ArrayList<>(fragment.body().childNodes())) value.appendChild(child.clone());
    }

    private void appendPlainText(Element target, String content) {
        String[] lines = (content == null ? "" : content).split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) target.appendElement("br");
            target.appendText(lines[index]);
        }
    }

    private void prepareMath(Element root, ContentLocation location) {
        int formulaIndex = 0;
        for (Element math : root.select("[data-latex]")) {
            boolean block = "block-math".equals(math.attr("data-type"));
            if (block) math.addClass("math-block");
            math.attr("data-pdf-field-path", formulaFieldPath(location, formulaIndex++));
            math.attr("data-pdf-question-id", location.questionId().toString());
        }
    }

    private void prepareImages(Element root, ContentLocation location, Map<String, RichContentAsset> ownedAssets,
            Map<String, PreparedImage> imageCache) {
        List<Element> images = new ArrayList<>(root.select("img"));
        for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
            Element image = images.get(imageIndex);
            String source = image.attr("src");
            String alt = image.attr("alt");
            RichContentAsset asset = ownedAssets.get(source);
            if (asset == null) throw imageFailure(location, imageIndex, alt, "unowned", "Ảnh không thuộc nội dung câu hỏi", null);
            PreparedImage prepared;
            try {
                prepared = imageCache.computeIfAbsent(asset.storageKey(), ignored -> prepareAsset(asset));
            } catch (ImagePreparationException ex) {
                throw imageFailure(location, imageIndex, alt, asset.storageKey(), ex.getMessage(), ex);
            }
            image.attr("src", "data:" + prepared.mediaType() + ";base64," + Base64.getEncoder().encodeToString(prepared.bytes()));
            normalizeImageWidth(image);
        }
    }

    private void normalizeImageWidth(Element image) {
        String value = image.attr("data-image-width");
        if (value.isBlank()) return;
        try {
            int width = Integer.parseInt(value);
            width = Math.max(IMAGE_MIN_WIDTH, Math.min(IMAGE_MAX_WIDTH, width));
            image.attr("data-image-width", String.valueOf(width));
            image.attr("style", "width: " + width + "%; height: auto;");
        } catch (NumberFormatException ignored) {
            image.removeAttr("data-image-width");
            image.removeAttr("style");
        }
    }

    private Map<String, RichContentAsset> ownedAssets(UUID questionId) {
        Map<String, RichContentAsset> result = new LinkedHashMap<>();
        for (RichContentAsset asset : assets.findByQuestionId(questionId)) result.put(storage.publicUrl(asset.storageKey()), asset);
        return result;
    }

    private PreparedImage prepareAsset(RichContentAsset asset) {
        byte[] bytes;
        try {
            bytes = storage.read(asset.storageKey());
        } catch (RuntimeException ex) {
            throw new ImagePreparationException("Không đọc được object từ storage", ex);
        }
        if (bytes.length != asset.byteSize()) throw new ImagePreparationException("Kích thước ảnh không khớp metadata");
        if (!sha256(bytes).equalsIgnoreCase(asset.sha256())) throw new ImagePreparationException("Checksum ảnh không khớp metadata");
        String detected = detectMediaType(bytes);
        if (!detected.equalsIgnoreCase(asset.mediaType())) throw new ImagePreparationException("MIME ảnh không khớp chữ ký file");
        if ("image/svg+xml".equals(detected)) validateSvg(new String(bytes, StandardCharsets.UTF_8));
        return new PreparedImage(detected, bytes);
    }

    private void validateSvg(String svg) {
        String lower = svg.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("<!doctype") || lower.contains("<!entity") || lower.contains("<script")
                || lower.contains("<foreignobject") || lower.contains("javascript:")) {
            throw new ImagePreparationException("SVG chứa nội dung không an toàn");
        }
        if (!lower.startsWith("<svg") && !(lower.startsWith("<?xml") && lower.contains("<svg"))) {
            throw new ImagePreparationException("SVG không hợp lệ");
        }
    }

    private String detectMediaType(byte[] bytes) {
        if (startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return "image/png";
        if (startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) return "image/jpeg";
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII)) || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) return "image/gif";
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

    private void ensureRendererReady(Page page) throws IOException {
        JsonNode status = JSON.readTree(String.valueOf(page.evaluate("""
                () => JSON.stringify({
                  compatMode: document.compatMode,
                  katexRenderType: typeof window.katex?.render
                })
                """)));
        String compatMode = status.path("compatMode").asText();
        if (!"CSS1Compat".equals(compatMode)) {
            throw unavailable("Chromium did not enter standards mode (compatMode=" + compatMode + ")", null);
        }
        if (!"function".equals(status.path("katexRenderType").asText())) {
            throw unavailable("Bundled KaTeX runtime was not loaded", null);
        }
    }

    private void ensureFormulasRendered(Page page, UUID variantId) throws IOException {
        String errors = String.valueOf(page.evaluate("""
                () => JSON.stringify(Array.from(document.querySelectorAll('[data-latex]')).flatMap((node) => {
                  try {
                    katex.render(node.dataset.latex || '', node, {
                      displayMode: node.dataset.type === 'block-math', throwOnError: true,
                      trust: false, maxExpand: 1000, strict: 'error'
                    });
                    return [];
                  } catch (error) {
                    return [{ fieldPath: node.dataset.pdfFieldPath, questionId: node.dataset.pdfQuestionId,
                      latex: node.dataset.latex || '', message: String(error.message || error) }];
                  }
                }))
                """));
        JsonNode failures = JSON.readTree(errors);
        if (!failures.isArray() || failures.isEmpty()) return;
        JsonNode failure = failures.get(0);
        UUID questionId = UUID.fromString(failure.path("questionId").asText());
        String fieldPath = failure.path("fieldPath").asText();
        String message = "Không thể render công thức tại " + fieldPath + ".";
        throw new VariantPdfFormulaException(message, fieldPath, variantId, questionId,
                List.of("latex=" + failure.path("latex").asText(), "reason=" + failure.path("message").asText()));
    }

    private void ensureKatexFontsReady(Page page) throws IOException {
        JsonNode status = JSON.readTree(String.valueOf(page.evaluate("""
                async () => {
                  const formulaCount = document.querySelectorAll('[data-latex]').length;
                  if (formulaCount === 0) return JSON.stringify({ formulaCount, fontFaces: [], loadErrors: [] });

                  const katexFaces = Array.from(document.fonts).filter((face) =>
                    String(face.family).replace(/^["']|["']$/g, '').startsWith('KaTeX_'));
                  const sample = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
                    + ' +-=/()[]{}\\\\^_\\u2202\\u2211\\u222b\\u221a\\u03c0';
                  const loads = await Promise.allSettled(katexFaces.map((face) =>
                    document.fonts.load(`${face.style} ${face.weight} ${face.stretch} 16px ${face.family}`, sample)));
                  await document.fonts.ready;
                  return JSON.stringify({
                    formulaCount,
                    fontFaces: katexFaces.map((face) => ({ family: face.family, status: face.status })),
                    loadErrors: loads.flatMap((result, index) => result.status === 'rejected'
                      ? [`${katexFaces[index].family}: ${String(result.reason)}`] : [])
                  });
                }
                """)));
        if (status.path("formulaCount").asInt() == 0) return;

        List<String> failures = new ArrayList<>();
        JsonNode fontFaces = status.path("fontFaces");
        if (!fontFaces.isArray() || fontFaces.isEmpty()) failures.add("No KaTeX font faces were registered");
        for (JsonNode face : fontFaces) {
            if (!"loaded".equals(face.path("status").asText())) {
                failures.add(face.path("family").asText("unknown") + "=" + face.path("status").asText("unknown"));
            }
        }
        for (JsonNode error : status.path("loadErrors")) failures.add(error.asText());
        if (!failures.isEmpty()) {
            throw unavailable("KaTeX fonts could not be loaded before PDF export: " + String.join(", ", failures), null);
        }
    }

    private Browser browser() {
        Browser current = browser;
        if (current != null && current.isConnected()) return current;
        synchronized (this) {
            current = browser;
            if (current != null && current.isConnected()) return current;
            if (playwright == null) playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
            if (chromiumExecutable != null && !chromiumExecutable.isBlank()) options.setExecutablePath(java.nio.file.Path.of(chromiumExecutable));
            browser = playwright.chromium().launch(options);
            return browser;
        }
    }

    private VariantPdfRenderUnavailableException unavailable(String message, Throwable cause) {
        return new VariantPdfRenderUnavailableException(message, cause);
    }

    private VariantPdfImageException imageFailure(ContentLocation location, int imageIndex, String alt,
            String storageKey, String reason, Throwable cause) {
        String answer = location.answerIndex() == null ? "" : ", đáp án " + (char) ('A' + location.answerIndex());
        String safeAlt = alt == null || alt.isBlank() ? "không có alt" : alt;
        String message = "Không thể tải ảnh tại Câu " + (location.questionIndex() + 1) + answer + ", ảnh " + (imageIndex + 1) + " (" + safeAlt + ").";
        String fieldPath = "questions[" + location.questionIndex() + "]"
                + (location.answerIndex() == null ? ".content" : ".answers[" + location.answerIndex() + "]")
                + ".images[" + imageIndex + "]";
        return new VariantPdfImageException(message, fieldPath, location.variantId(), location.questionId(),
                List.of("alt=" + safeAlt, "storageKey=" + storageKey, "reason=" + reason), cause);
    }

    private String formulaFieldPath(ContentLocation location, int formulaIndex) {
        return "questions[" + location.questionIndex() + "]"
                + (location.answerIndex() == null ? ".content" : ".answers[" + location.answerIndex() + "]")
                + ".formulas[" + formulaIndex + "]";
    }

    private String katexJavascript() throws IOException { return resource(KATEX_BASE + "katex.min.js"); }

    private String katexStylesheet() {
        try {
            String css = resource(KATEX_BASE + "katex.min.css");
            Matcher matcher = KATEX_FONT_URL.matcher(css);
            StringBuffer rendered = new StringBuffer();
            while (matcher.find()) {
                String filename = matcher.group(1);
                String mimeType = filename.endsWith(".woff2") ? "font/woff2" : "font/woff";
                String data = Base64.getEncoder().encodeToString(resourceBytes(KATEX_BASE + "fonts/" + filename));
                matcher.appendReplacement(rendered, Matcher.quoteReplacement("url(data:" + mimeType + ";base64," + data + ")"));
            }
            matcher.appendTail(rendered);
            return rendered.toString();
        } catch (IOException ex) {
            throw unavailable("Bundled KaTeX assets are unavailable", ex);
        }
    }

    private String resource(String path) throws IOException { return new String(resourceBytes(path), StandardCharsets.UTF_8); }

    private byte[] resourceBytes(String path) throws IOException {
        try (InputStream input = VariantPdfRenderer.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing classpath resource " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    @PreDestroy
    void closeBrowser() {
        synchronized (this) {
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            browser = null;
            playwright = null;
        }
    }

    private record ContentLocation(UUID variantId, UUID questionId, int questionIndex, Integer answerIndex) { }
    private record PreparedImage(String mediaType, byte[] bytes) { }

    private static final class ImagePreparationException extends RuntimeException {
        private ImagePreparationException(String message) { super(message); }
        private ImagePreparationException(String message, Throwable cause) { super(message, cause); }
    }
}
