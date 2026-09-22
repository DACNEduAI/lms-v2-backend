package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

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

import edu.ai.lms.core.quiz3.app.domain.assessment.AssessmentModels.AnswerView;
import edu.ai.lms.core.quiz3.app.domain.assessment.AssessmentModels.VariantQuestionView;
import edu.ai.lms.core.quiz3.app.domain.assessment.AssessmentModels.VariantView;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz3.app.exception.VariantPdfFormulaException;
import edu.ai.lms.core.quiz3.app.exception.VariantPdfImageException;
import edu.ai.lms.core.quiz3.app.exception.VariantPdfRenderUnavailableException;
import edu.ai.lms.core.quiz3.app.port.out.RichContentAssetStoragePort;
import edu.ai.lms.core.quiz3.api.CodeRunnerContractSnapshotApi.PrintableContract;
import edu.ai.lms.core.quiz3.api.CodeRunnerContractSnapshotApi.PublicExample;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PreDestroy;

/** Dedicated Quiz3 PDF renderer. Asset lookup is intentionally Quiz3-only. */
@Service
@RequiredArgsConstructor
public class Quiz3PdfRenderer {
    private static final String KATEX_BASE = "/META-INF/resources/webjars/katex/0.16.22/dist/";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern FONT_URL = Pattern.compile("url\\((?:['\\\"])?fonts/([^)'\\\"]+)(?:['\\\"])?\\)");
    private static final Pattern FENCED_CODE = Pattern.compile("(?s)```(?:[A-Za-z0-9_+.#-]+)?\\s*\\R(.*?)```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final int IMAGE_MIN_WIDTH = 10;
    private static final int IMAGE_MAX_WIDTH = 100;
    private static final String CSS = """
            @page { size: A4 portrait; margin: 14mm 14mm 16mm; @bottom-right { content: "Trang " counter(page) " / " counter(pages); font: 8pt 'Noto Sans', sans-serif; color: #555; } }
            html, body { margin: 0; padding: 0; background: #fff; color: #111; }
            body { font-family: 'Noto Sans', sans-serif; font-size: 10.5pt; line-height: 1.45; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
            .exam-header { border-bottom: .5mm solid #111; padding-bottom: 4mm; margin-bottom: 5mm; }
            .course-line { text-align: center; font-size: 9.5pt; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; }
            .exam-title { margin: 2mm 0 0; text-align: center; font-size: 17pt; line-height: 1.2; text-transform: uppercase; }
            .assessment-title { margin: 1mm 0 0; text-align: center; font-size: 11pt; font-weight: 600; }
            .exam-meta { display: flex; justify-content: center; gap: 9mm; margin-top: 2.5mm; font-size: 9.5pt; }
            .candidate { display: grid; grid-template-columns: 1fr 55mm; gap: 3mm 8mm; margin: 5mm 0 4mm; }
            .candidate-line { min-height: 7mm; border-bottom: .25mm dotted #555; }
            .instructions { border: .3mm solid #777; border-radius: 1.5mm; background: #f5f5f5; padding: 3mm 4mm; font-size: 9.5pt; }
            .instructions strong { display: block; margin-bottom: 1mm; text-transform: uppercase; }
            .question { margin: 0 0 7mm 0; break-inside: auto; page-break-inside: auto; }
            .question-title { display: flex; justify-content: space-between; gap: 5mm; margin: 0 0 2.5mm; padding-bottom: 1mm; border-bottom: .3mm solid #999; font-size: 11.5pt; font-weight: 700; }
            .row { display: table; width: 100%; table-layout: fixed; }
            .label, .content { display: table-cell; vertical-align: top; }
            .label { width: 15mm; font-weight: bold; white-space: nowrap; }
            .answers { margin: 3mm 0 0 5mm; padding: 0; }
            .answer { margin: 0 0 2.5mm 0; break-inside: avoid; page-break-inside: avoid; }
            .content > :first-child { margin-top: 0; } .content > :last-child { margin-bottom: 0; }
            p { margin: 0 0 2mm 0; } ul, ol { margin-top: 1mm; margin-bottom: 2mm; }
            table { width: 100%; border-collapse: collapse; break-inside: auto; page-break-inside: auto; }
            th, td { border: .25mm solid #777; padding: 1.5mm; vertical-align: top; }
            tr, pre, blockquote { break-inside: avoid; page-break-inside: avoid; }
            pre.code-block { margin: 3mm 0; border: .3mm solid #555; border-left: 1.2mm solid #333; border-radius: 1mm; background: #f6f7f8; padding: 3mm; white-space: pre-wrap; overflow-wrap: anywhere; font: 8.7pt/1.45 Consolas, 'Courier New', monospace; }
            code.inline-code { padding: .2mm 1mm; border-radius: .7mm; background: #eee; font: 9pt Consolas, 'Courier New', monospace; }
            .runner-meta { margin: 2mm 0; font-size: 9pt; color: #444; }
            .examples { margin: 3mm 0; font-size: 9pt; }
            .examples caption { text-align: left; font-weight: 700; margin-bottom: 1mm; }
            .examples th { background: #eee; }
            .answer-area { min-height: 110mm; margin-top: 4mm; border: .3mm solid #777; border-radius: 1mm; padding: 3mm; background: repeating-linear-gradient(to bottom, transparent 0, transparent 7mm, #c8c8c8 7.1mm, transparent 7.3mm); break-inside: avoid; page-break-inside: avoid; }
            .answer-area-label { display: inline-block; padding-right: 2mm; background: #fff; font-size: 9pt; font-weight: 700; }
            .question + .question { margin-top: 5mm; }
            .mode-write-on-exam .question.code-runner { break-before: page; page-break-before: always; }
            .mode-write-on-exam .exam-header + .question.code-runner { break-before: auto; page-break-before: auto; }
            img { display: inline-block; height: auto; max-width: 100%; margin: .15rem .25rem; vertical-align: middle; break-inside: avoid; page-break-inside: avoid; }
            img[data-image-layout=block] { display: block; margin: 1.5mm 0; }
            .math-block { display: block; width: 100%; margin: 1.5mm 0; text-align: center; }
            .katex-display { margin: 0; }
            """;

    private final Quiz3RichContentAssetRepository assets;
    private final RichContentAssetStoragePort storage;
    private final Semaphore renderSlots = new Semaphore(1);
    @Value("${quiz.pdf.render.timeout-seconds:30}") private long timeoutSeconds;
    @Value("${quiz.pdf.chromium.executable:}") private String chromiumExecutable;
    private volatile Playwright playwright;
    private volatile Browser browser;

    public byte[] render(VariantView variant) {
        return render(defaultDocument(variant));
    }

    public byte[] render(PrintDocument document) {
        boolean acquired = false;
        try {
            acquired = renderSlots.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) throw unavailable("PDF renderer is busy. Please try again.", null);
            try (BrowserContext context = browser().newContext()) {
                context.setDefaultTimeout(Duration.ofSeconds(timeoutSeconds).toMillis());
                context.route("**/*", route -> route.abort());
                Page page = context.newPage();
                page.setContent(buildHtml(document), new Page.SetContentOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.addScriptTag(new Page.AddScriptTagOptions().setContent(katexJavascript()));
                ensureRendererReady(page); ensureFormulasRendered(page, document.variant().id()); ensureKatexFontsReady(page);
                return page.pdf(new Page.PdfOptions().setFormat("A4").setPrintBackground(true).setPreferCSSPageSize(true)
                        .setMargin(new Margin().setTop("0").setRight("0").setBottom("0").setLeft("0")));
            }
        } catch (VariantPdfFormulaException | VariantPdfImageException | VariantPdfRenderUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw unavailable("PDF rendering was interrupted", ex);
        } catch (PlaywrightException | IOException ex) { closeBrowser(); throw unavailable("Chromium could not render this PDF", ex);
        } catch (RuntimeException ex) { throw unavailable("Chromium could not render this PDF", ex);
        } finally { if (acquired) renderSlots.release();
        }
    }

    String buildHtml(VariantView variant) {
        return buildHtml(defaultDocument(variant));
    }

    String buildHtml(PrintDocument printDocument) {
        VariantView variant = printDocument.variant();
        Document document = Document.createShell("");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(Entities.EscapeMode.xhtml).prettyPrint(false).charset(StandardCharsets.UTF_8);
        document.selectFirst("html").attr("xmlns", "http://www.w3.org/1999/xhtml");
        document.body().addClass("mode-" + printDocument.answerMode().name().toLowerCase(Locale.ROOT).replace('_', '-'));
        document.head().appendElement("meta").attr("charset", "UTF-8");
        document.head().appendElement("style").attr("type", "text/css").appendChild(new DataNode(CSS + "\n" + katexStylesheet()));
        appendExamHeader(document.body(), printDocument);
        Map<String, PreparedImage> cache = new HashMap<>();
        for (int questionIndex = 0; questionIndex < variant.questions().size(); questionIndex++) {
            VariantQuestionView question = variant.questions().get(questionIndex);
            PrintQuestion printQuestion = printDocument.questions().stream()
                    .filter(value -> value.question().id().equals(question.id())).findFirst()
                    .orElse(new PrintQuestion(question, null));
            Map<String, Quiz3RichContentAsset> owned = ownedAssets(question.id());
            Element questionElement = document.body().appendElement("section").addClass("question");
            Element title = questionElement.appendElement("div").addClass("question-title");
            title.appendElement("span").text("Câu " + (questionIndex + 1) + ":");
            if (printQuestion.contract() != null && printQuestion.contract().points() > 0) {
                title.appendElement("span").text(formatPoints(printQuestion.contract().points()) + " điểm");
            }
            Location location = new Location(variant.id(), question.id(), questionIndex, null);
            if ("CODE_RUNNER".equals(question.type())) {
                questionElement.addClass("code-runner");
                appendCodeRunnerQuestion(questionElement, printQuestion, location, owned, cache);
                if (printDocument.answerMode() == AnswerMode.WRITE_ON_EXAM) appendAnswerArea(questionElement);
            } else {
                appendContent(questionElement, question.content(), question.contentFormat(), location, owned, cache);
                Element answers = questionElement.appendElement("div").addClass("answers");
                for (int answerIndex = 0; answerIndex < question.answers().size(); answerIndex++) {
                    AnswerView answer = question.answers().get(answerIndex);
                    appendRow(answers.appendElement("div").addClass("answer"), String.valueOf((char) ('A' + answerIndex)) + ".",
                            answer.content(), answer.contentFormat(), new Location(variant.id(), question.id(), questionIndex, answerIndex), owned, cache);
                }
            }
        }
        return "<!doctype html>\n" + document.outerHtml();
    }

    private void appendExamHeader(Element body, PrintDocument document) {
        Element header = body.appendElement("header").addClass("exam-header");
        String course = List.of(document.courseCode(), document.courseName()).stream()
                .filter(value -> value != null && !value.isBlank()).reduce((left, right) -> left + " · " + right)
                .orElse("Môn học");
        header.appendElement("div").addClass("course-line").text(course);
        header.appendElement("h1").addClass("exam-title").text("Đề thi lập trình");
        if (document.assessmentTitle() != null && !document.assessmentTitle().isBlank()) {
            header.appendElement("div").addClass("assessment-title").text(document.assessmentTitle());
        }
        Element meta = header.appendElement("div").addClass("exam-meta");
        meta.appendElement("span").appendText("Mã đề: ").appendElement("strong").text(document.variantCode());
        meta.appendElement("span").appendText("Thời gian: ").appendElement("strong")
                .text(document.timeLimitMinutes() == null ? "Theo thông báo" : document.timeLimitMinutes() + " phút");
        double totalPoints = document.questions().stream().filter(value -> value.contract() != null)
                .mapToDouble(value -> value.contract().points()).sum();
        if (totalPoints > 0) meta.appendElement("span").appendText("Tổng điểm: ").appendElement("strong").text(formatPoints(totalPoints));

        Element candidate = header.appendElement("div").addClass("candidate");
        candidate.appendElement("div").addClass("candidate-line").text("Họ và tên: ");
        candidate.appendElement("div").addClass("candidate-line").text("MSSV: ");
        candidate.appendElement("div").addClass("candidate-line").text("Lớp: ");
        candidate.appendElement("div").addClass("candidate-line").text("Phòng thi: ");

        Element instructions = header.appendElement("div").addClass("instructions");
        instructions.appendElement("strong").text("Hướng dẫn làm bài");
        instructions.appendElement("div").text(document.answerMode().instruction());
        instructions.appendElement("div").text("Chỉ cài đặt đúng hàm được yêu cầu; không thay đổi chữ ký hàm. Các test ẩn không xuất hiện trong đề.");
    }

    private void appendCodeRunnerQuestion(Element parent, PrintQuestion printable, Location location,
            Map<String, Quiz3RichContentAsset> owned, Map<String, PreparedImage> cache) {
        VariantQuestionView question = printable.question();
        PrintableContract contract = printable.contract();
        String content = question.content() == null ? "" : question.content();
        String statement = content;
        String fencedStarter = null;
        if (question.contentFormat() == null || question.contentFormat() == RichContentFormat.PLAIN_TEXT) {
            Matcher fenced = FENCED_CODE.matcher(content);
            if (fenced.find()) fencedStarter = fenced.group(1).strip();
            statement = fenced.replaceAll("").strip();
            if (contract != null && (statement.isBlank() || looksLikeCode(statement))) statement = contract.statement();
            appendMarkdownLikeText(parent.appendElement("div").addClass("content"), statement);
        } else {
            appendContent(parent, content, question.contentFormat(), location, owned, cache);
        }

        String entryPoint = contract == null ? "" : contract.entryPoint();
        if (entryPoint != null && !entryPoint.isBlank()) {
            parent.appendElement("div").addClass("runner-meta")
                    .text("Ngôn ngữ: C++17 · Hàm cần cài đặt: " + entryPoint);
        }
        String starter = contract != null && contract.starterCode() != null && !contract.starterCode().isBlank()
                ? contract.starterCode().strip() : fencedStarter;
        if (starter != null && !starter.isBlank()) {
            parent.appendElement("pre").addClass("code-block").appendElement("code").text(starter);
        }
        if (contract != null && contract.publicExamples() != null && !contract.publicExamples().isEmpty()) {
            appendPublicExamples(parent, contract.publicExamples());
        }
    }

    private void appendPublicExamples(Element parent, List<PublicExample> examples) {
        Element table = parent.appendElement("table").addClass("examples");
        table.appendElement("caption").text("Ví dụ công khai");
        Element header = table.appendElement("thead").appendElement("tr");
        header.appendElement("th").text("Đầu vào");
        header.appendElement("th").text("Kết quả mong đợi");
        Element body = table.appendElement("tbody");
        for (PublicExample example : examples) {
            Element row = body.appendElement("tr");
            String arguments = example.arguments().entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + entry.getValue().toString())
                    .reduce((left, right) -> left + "; " + right).orElse("—");
            row.appendElement("td").text(arguments);
            row.appendElement("td").text(example.expectedOutput() == null ? "—" : example.expectedOutput().toString());
        }
    }

    private void appendAnswerArea(Element parent) {
        Element area = parent.appendElement("div").addClass("answer-area");
        area.appendElement("span").addClass("answer-area-label").text("Phần bài làm của thí sinh");
    }

    private void appendMarkdownLikeText(Element target, String content) {
        String normalized = content == null ? "" : content.strip();
        if (normalized.isBlank()) return;
        for (String paragraph : normalized.split("(?:\\R\\s*){2,}")) {
            Element p = target.appendElement("p");
            Matcher inline = INLINE_CODE.matcher(paragraph);
            int offset = 0;
            while (inline.find()) {
                p.appendText(paragraph.substring(offset, inline.start()));
                p.appendElement("code").addClass("inline-code").text(inline.group(1));
                offset = inline.end();
            }
            p.appendText(paragraph.substring(offset));
        }
    }

    private boolean looksLikeCode(String value) {
        String normalized = value == null ? "" : value.strip();
        return normalized.startsWith("#include") || normalized.startsWith("std::")
                || normalized.matches("(?s)^[A-Za-z_][A-Za-z0-9_:<> ,*&]*\\([^)]*\\)\\s*\\{.*");
    }

    private String formatPoints(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
    }

    private void appendContent(Element target, String content, RichContentFormat format, Location location,
            Map<String, Quiz3RichContentAsset> owned, Map<String, PreparedImage> cache) {
        if (format == null || format == RichContentFormat.PLAIN_TEXT) { appendPlainText(target, content); return; }
        Document fragment = Jsoup.parseBodyFragment(content == null ? "" : content);
        prepareMath(fragment.body(), location); prepareImages(fragment.body(), location, owned, cache);
        for (Node child : new ArrayList<>(fragment.body().childNodes())) target.appendChild(child.clone());
    }

    private void appendRow(Element parent, String label, String content, RichContentFormat format, Location location,
            Map<String, Quiz3RichContentAsset> owned, Map<String, PreparedImage> cache) {
        Element row = parent.appendElement("div").addClass("row"); row.appendElement("div").addClass("label").text(label);
        Element value = row.appendElement("div").addClass("content");
        appendContent(value, content, format, location, owned, cache);
    }

    private void appendPlainText(Element target, String content) {
        String[] lines = (content == null ? "" : content).split("\\R", -1);
        for (int index = 0; index < lines.length; index++) { if (index > 0) target.appendElement("br"); target.appendText(lines[index]); }
    }

    private void prepareMath(Element root, Location location) {
        int index = 0;
        for (Element math : root.select("[data-latex]")) {
            if ("block-math".equals(math.attr("data-type"))) math.addClass("math-block");
            math.attr("data-pdf-field-path", fieldPath(location, "formulas", index++));
            math.attr("data-pdf-question-id", location.questionId.toString());
        }
    }

    private void prepareImages(Element root, Location location, Map<String, Quiz3RichContentAsset> owned,
            Map<String, PreparedImage> cache) {
        int index = 0;
        for (Element image : new ArrayList<>(root.select("img"))) {
            Quiz3RichContentAsset asset = owned.get(image.attr("src"));
            if (asset == null) throw imageFailure(location, index, image.attr("alt"), "unowned", "Ảnh không thuộc nội dung câu hỏi", null);
            try {
                PreparedImage prepared = cache.computeIfAbsent(asset.storageKey(), ignored -> prepare(asset));
                image.attr("src", "data:" + prepared.mediaType + ";base64," + Base64.getEncoder().encodeToString(prepared.bytes));
            } catch (ImagePreparationException ex) { throw imageFailure(location, index, image.attr("alt"), asset.storageKey(), ex.getMessage(), ex); }
            normalizeWidth(image); index++;
        }
    }

    private Map<String, Quiz3RichContentAsset> ownedAssets(UUID questionId) {
        Map<String, Quiz3RichContentAsset> result = new LinkedHashMap<>();
        for (Quiz3RichContentAsset asset : assets.findByQuestionId(questionId)) result.put(storage.publicUrl(asset.storageKey()), asset);
        return result;
    }

    private PreparedImage prepare(Quiz3RichContentAsset asset) {
        byte[] bytes;
        try { bytes = storage.read(asset.storageKey()); } catch (RuntimeException ex) { throw new ImagePreparationException("Không đọc được object từ storage", ex); }
        if (bytes.length != asset.byteSize()) throw new ImagePreparationException("Kích thước ảnh không khớp metadata");
        if (!sha256(bytes).equalsIgnoreCase(asset.sha256())) throw new ImagePreparationException("Checksum ảnh không khớp metadata");
        String type = detectMediaType(bytes);
        if (!type.equalsIgnoreCase(asset.mediaType())) throw new ImagePreparationException("MIME ảnh không khớp chữ ký file");
        if ("image/svg+xml".equals(type)) validateSvg(new String(bytes, StandardCharsets.UTF_8));
        return new PreparedImage(type, bytes);
    }

    private void normalizeWidth(Element image) {
        String value = image.attr("data-image-width"); if (value.isBlank()) return;
        try { int width = Math.max(IMAGE_MIN_WIDTH, Math.min(IMAGE_MAX_WIDTH, Integer.parseInt(value))); image.attr("data-image-width", String.valueOf(width)); image.attr("style", "width: " + width + "% ; height: auto;"); }
        catch (NumberFormatException ex) { image.removeAttr("data-image-width"); image.removeAttr("style"); }
    }

    private void validateSvg(String svg) {
        String lower = svg.trim().toLowerCase(Locale.ROOT);
        if ((!lower.startsWith("<svg") && !(lower.startsWith("<?xml") && lower.contains("<svg"))) || lower.contains("<!doctype") || lower.contains("<!entity") || lower.contains("<script") || lower.contains("<foreignobject") || lower.contains("javascript:")) throw new ImagePreparationException("SVG chứa nội dung không an toàn hoặc không hợp lệ");
    }
    private String detectMediaType(byte[] bytes) {
        if (startsWith(bytes, new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a})) return "image/png";
        if (startsWith(bytes, new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff})) return "image/jpeg";
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII)) || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) return "image/gif";
        if (startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII)) && bytes.length > 12 && "WEBP".equals(new String(bytes,8,4,StandardCharsets.US_ASCII))) return "image/webp";
        String text = new String(bytes, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("<svg") || text.startsWith("<?xml") && text.contains("<svg")) return "image/svg+xml";
        throw new ImagePreparationException("Loại ảnh không được hỗ trợ");
    }
    private static boolean startsWith(byte[] source, byte[] prefix) { if (source.length < prefix.length) return false; for (int i=0;i<prefix.length;i++) if (source[i]!=prefix[i]) return false; return true; }
    private static String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); } }
    private void ensureRendererReady(Page page) throws IOException {
        JsonNode status = JSON.readTree(String.valueOf(page.evaluate("""
                () => JSON.stringify({ compatMode: document.compatMode, katexRenderType: typeof window.katex?.render })
                """)));
        if (!"CSS1Compat".equals(status.path("compatMode").asText())) throw unavailable("Chromium did not enter standards mode", null);
        if (!"function".equals(status.path("katexRenderType").asText())) throw unavailable("Bundled KaTeX runtime was not loaded", null);
    }
    private void ensureFormulasRendered(Page page, UUID variantId) throws IOException {
        JsonNode failures = JSON.readTree(String.valueOf(page.evaluate("""
                () => JSON.stringify(Array.from(document.querySelectorAll('[data-latex]')).flatMap((node) => {
                  try { katex.render(node.dataset.latex || '', node, { displayMode: node.dataset.type === 'block-math', throwOnError: true, trust: false, maxExpand: 1000, strict: 'error' }); return []; }
                  catch (error) { return [{ fieldPath: node.dataset.pdfFieldPath, questionId: node.dataset.pdfQuestionId, latex: node.dataset.latex || '', message: String(error.message || error) }]; }
                }))
                """)));
        if (!failures.isArray() || failures.isEmpty()) return;
        JsonNode failure = failures.get(0);
        throw new VariantPdfFormulaException("Không thể render công thức tại " + failure.path("fieldPath").asText() + ".",
                failure.path("fieldPath").asText(), variantId, UUID.fromString(failure.path("questionId").asText()),
                List.of("latex=" + failure.path("latex").asText(), "reason=" + failure.path("message").asText()));
    }
    private void ensureKatexFontsReady(Page page) throws IOException {
        JsonNode status = JSON.readTree(String.valueOf(page.evaluate("""
                async () => { const formulaCount=document.querySelectorAll('[data-latex]').length; if(formulaCount===0)return JSON.stringify({formulaCount,fontFaces:[],loadErrors:[]});
                const faces=Array.from(document.fonts).filter((face)=>String(face.family).replace(/^["']|["']$/g,'').startsWith('KaTeX_'));
                await Promise.allSettled(faces.map((face)=>document.fonts.load(`${face.style} ${face.weight} ${face.stretch} 16px ${face.family}`,'0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'))); await document.fonts.ready;
                return JSON.stringify({formulaCount,fontFaces:faces.map((face)=>({family:face.family,status:face.status}))}); }
                """)));
        if (status.path("formulaCount").asInt() == 0) return;
        List<String> failures = new ArrayList<>();
        JsonNode faces = status.path("fontFaces");
        if (!faces.isArray() || faces.isEmpty()) failures.add("No KaTeX font faces were registered");
        for (JsonNode face : faces) if (!"loaded".equals(face.path("status").asText())) failures.add(face.path("family").asText("unknown") + "=" + face.path("status").asText("unknown"));
        if (!failures.isEmpty()) throw unavailable("KaTeX fonts could not be loaded before PDF export: " + String.join(", ", failures), null);
    }
    private Browser browser() {
        Browser current = browser; if (current != null && current.isConnected()) return current;
        synchronized (this) { current = browser; if (current != null && current.isConnected()) return current; if (playwright == null) playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true); if (chromiumExecutable != null && !chromiumExecutable.isBlank()) options.setExecutablePath(java.nio.file.Path.of(chromiumExecutable));
            browser = playwright.chromium().launch(options); return browser; }
    }
    private VariantPdfRenderUnavailableException unavailable(String message, Throwable cause) { return new VariantPdfRenderUnavailableException(message, cause); }
    private VariantPdfImageException imageFailure(Location location, int imageIndex, String alt, String key, String reason, Throwable cause) {
        String answer = location.answerIndex == null ? "" : ", đáp án " + (char)('A' + location.answerIndex);
        String path = fieldPath(location, "images", imageIndex);
        return new VariantPdfImageException("Không thể tải ảnh tại Câu " + (location.questionIndex + 1) + answer + ", ảnh " + (imageIndex + 1) + ".", path, location.variantId, location.questionId, List.of("alt=" + (alt == null ? "" : alt), "storageKey=" + key, "reason=" + reason), cause);
    }
    private String fieldPath(Location location, String type, int index) { return "questions[" + location.questionIndex + "]" + (location.answerIndex == null ? ".content" : ".answers[" + location.answerIndex + "]") + "." + type + "[" + index + "]"; }
    private String katexStylesheet() { try { String css = resource(KATEX_BASE + "katex.min.css"); Matcher matcher = FONT_URL.matcher(css); StringBuffer result = new StringBuffer(); while (matcher.find()) { String name=matcher.group(1); String mime=name.endsWith(".woff2")?"font/woff2":"font/woff"; matcher.appendReplacement(result, Matcher.quoteReplacement("url(data:"+mime+";base64,"+Base64.getEncoder().encodeToString(resourceBytes(KATEX_BASE+"fonts/"+name))+")")); } matcher.appendTail(result); return result.toString(); } catch (IOException ex) { throw new VariantPdfRenderUnavailableException("Bundled KaTeX assets are unavailable",ex); } }
    private String resource(String path) throws IOException { return new String(resourceBytes(path), StandardCharsets.UTF_8); }
    private byte[] resourceBytes(String path) throws IOException { try (InputStream input = Quiz3PdfRenderer.class.getResourceAsStream(path)) { if (input == null) throw new IOException("Missing classpath resource " + path); ByteArrayOutputStream output = new ByteArrayOutputStream(); input.transferTo(output); return output.toByteArray(); } }
    private String katexJavascript() throws IOException { return resource(KATEX_BASE + "katex.min.js"); }
    private PrintDocument defaultDocument(VariantView variant) {
        return new PrintDocument("Đề kiểm tra", "", "", "", variant.code(), null,
                AnswerMode.ONLINE_SUBMISSION, variant,
                variant.questions().stream().map(question -> new PrintQuestion(question, null)).toList());
    }
    @PreDestroy void closeBrowser() { synchronized (this) { if (browser != null) browser.close(); if (playwright != null) playwright.close(); browser = null; playwright = null; } }

    public enum AnswerMode {
        ONLINE_SUBMISSION("Thí sinh nộp mã nguồn trên hệ thống Code Runner. Bản in chỉ dùng để đọc đề và ghi chú; không chấm phần viết tay trên đề."),
        WRITE_ON_EXAM("Thí sinh viết mã nguồn vào vùng bài làm ngay sau từng câu hỏi. Viết rõ ràng và ghi tiếp sang mặt sau nếu thiếu chỗ."),
        SEPARATE_ANSWER_SHEET("Thí sinh làm bài trên giấy thi được phát riêng. Không viết bài làm vào đề và phải ghi rõ số câu trên mỗi tờ giấy.");

        private final String instruction;
        AnswerMode(String instruction) { this.instruction = instruction; }
        String instruction() { return instruction; }
    }

    public record PrintQuestion(VariantQuestionView question, PrintableContract contract) { }
    public record PrintDocument(
            String assessmentTitle,
            String description,
            String courseCode,
            String courseName,
            String variantCode,
            Integer timeLimitMinutes,
            AnswerMode answerMode,
            VariantView variant,
            List<PrintQuestion> questions) {
        public PrintDocument {
            answerMode = answerMode == null ? AnswerMode.ONLINE_SUBMISSION : answerMode;
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    private record Location(UUID variantId, UUID questionId, int questionIndex, Integer answerIndex) { }
    private record PreparedImage(String mediaType, byte[] bytes) { }
    private static final class ImagePreparationException extends RuntimeException { ImagePreparationException(String message) { super(message); } ImagePreparationException(String message, Throwable cause) { super(message,cause); } }
}
