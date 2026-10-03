package edu.ai.lms.core.quiz3.app.service;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz3.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz3.app.port.out.SanitizeRichContentPort;
import lombok.RequiredArgsConstructor;

/** Converts Quiz3 display content to the single persisted Tiptap HTML format. */
@Service
@RequiredArgsConstructor
public class Quiz3RichContentNormalizer {
    private static final Logger log = LoggerFactory.getLogger(Quiz3RichContentNormalizer.class);
    private static final Set<String> SUPPORTED_ELEMENTS = Set.of(
            "p", "br", "strong", "em", "u", "s", "code", "pre", "blockquote", "hr",
            "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "input",
            "span", "div", "a", "img", "table", "tbody", "thead", "tr", "th", "td",
            "colgroup", "col", "mark", "sub", "sup");
    private static final Predicate<String> NO_IMAGES = source -> false;

    private final SanitizeRichContentPort sanitizer;

    /** AI's webhook remains untyped. Recognizable safe HTML is retained; everything else becomes escaped text. */
    public RichTextValue normalizeAi(String content) {
        Inspection inspection = inspect(content, NO_IMAGES);
        if (inspection.kind() == InspectionKind.STRUCTURED_HTML) {
            return sanitizeHtml(content, NO_IMAGES);
        }
        if (inspection.kind() != InspectionKind.PLAIN_TEXT) {
            log.warn("Quiz3 AI content was normalized as text: reason={}, preview={}",
                    inspection.kind(), preview(content));
        }
        return plainText(content);
    }

    /** Content carrying an explicit format is normalized and validated at the LMS persistence boundary. */
    public RichTextValue normalizeDeclared(String content, String contentFormat,
            Predicate<String> allowedImageSource) {
        if ("TIPTAP_HTML".equals(contentFormat)) {
            return sanitizeHtml(content, allowedImageSource == null ? NO_IMAGES : allowedImageSource);
        }
        return plainText(content);
    }

    /**
     * Historical rows may have HTML mislabeled as plain text. Unsafe or genuinely ambiguous markup is left
     * untouched so the backfill can report it for manual review.
     */
    public HistoricalNormalization inspectHistorical(String content, String contentFormat,
            Predicate<String> allowedImageSource) {
        if (content == null) return HistoricalNormalization.skipped(HistoricalDecision.NULL_CONTENT);
        Predicate<String> images = allowedImageSource == null ? NO_IMAGES : allowedImageSource;
        if ("TIPTAP_HTML".equals(contentFormat)) {
            try {
                RichTextValue normalized = sanitizeHtml(content, images);
                return HistoricalNormalization.converted(normalized,
                        normalized.content().equals(content) ? HistoricalDecision.ALREADY_NORMALIZED
                                : HistoricalDecision.SANITIZED_HTML);
            } catch (RichContentValidationException ex) {
                return HistoricalNormalization.skipped(HistoricalDecision.UNOWNED_IMAGE);
            }
        }

        Inspection inspection = inspect(content, images);
        return switch (inspection.kind()) {
            case STRUCTURED_HTML -> HistoricalNormalization.converted(sanitizeHtml(content, images),
                    HistoricalDecision.RECOGNIZED_HTML);
            case PLAIN_TEXT -> HistoricalNormalization.converted(plainText(content), HistoricalDecision.PLAIN_TEXT);
            case UNSUPPORTED_MARKUP -> HistoricalNormalization.skipped(HistoricalDecision.AMBIGUOUS_MARKUP);
            case UNOWNED_IMAGE -> HistoricalNormalization.skipped(HistoricalDecision.UNOWNED_IMAGE);
        };
    }

    public RichTextValue appendOwnedImage(String content, String contentFormat, String imageHtml,
            Predicate<String> allowedImageSource) {
        Predicate<String> images = allowedImageSource == null ? NO_IMAGES : allowedImageSource;
        RichTextValue base = "TIPTAP_HTML".equals(contentFormat)
                ? sanitizeHtml(content, images)
                : normalizeLegacyForRuntime(content, images);
        return sanitizeHtml(base.content() + imageHtml, images);
    }

    private RichTextValue normalizeLegacyForRuntime(String content, Predicate<String> allowedImageSource) {
        Inspection inspection = inspect(content, allowedImageSource);
        if (inspection.kind() == InspectionKind.STRUCTURED_HTML) return sanitizeHtml(content, allowedImageSource);
        return plainText(content);
    }

    private RichTextValue sanitizeHtml(String content, Predicate<String> allowedImageSource) {
        Document original = parse(content);
        validateImages(original, allowedImageSource);
        String sanitized = sanitizer.sanitize(content == null ? "" : content);
        validateImages(parse(sanitized), allowedImageSource);
        return new RichTextValue(sanitized, RichContentFormat.TIPTAP_HTML);
    }

    private Inspection inspect(String content, Predicate<String> allowedImageSource) {
        Document document = parse(content);
        List<Element> elements = document.body().getAllElements().stream()
                .filter(element -> element != document.body())
                .toList();
        if (elements.isEmpty()) return new Inspection(InspectionKind.PLAIN_TEXT);
        if (elements.stream().anyMatch(element -> !SUPPORTED_ELEMENTS.contains(element.normalName()))) {
            return new Inspection(InspectionKind.UNSUPPORTED_MARKUP);
        }
        if (!imagesAreOwned(document, allowedImageSource)) return new Inspection(InspectionKind.UNOWNED_IMAGE);
        return new Inspection(InspectionKind.STRUCTURED_HTML);
    }

    private static Document parse(String content) {
        Document document = Jsoup.parseBodyFragment(content == null ? "" : content);
        document.outputSettings().prettyPrint(false);
        return document;
    }

    private static boolean imagesAreOwned(Document document, Predicate<String> allowedImageSource) {
        return document.select("img").stream()
                .allMatch(image -> image.hasAttr("src") && !image.attr("src").isBlank()
                        && allowedImageSource.test(image.attr("src")));
    }

    private static void validateImages(Document document, Predicate<String> allowedImageSource) {
        if (!imagesAreOwned(document, allowedImageSource)) {
            throw new RichContentValidationException("Rich content contains an external or unowned image URL");
        }
    }

    private static RichTextValue plainText(String content) {
        String source = content == null ? "" : content;
        String[] lines = source.split("\\R", -1);
        StringBuilder html = new StringBuilder();
        for (String line : lines) {
            html.append("<p>");
            if (line.isEmpty()) html.append("<br>");
            else html.append(escapeText(line));
            html.append("</p>");
        }
        return new RichTextValue(html.toString(), RichContentFormat.TIPTAP_HTML);
    }

    private static String escapeText(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String preview(String value) {
        String compact = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return compact.substring(0, Math.min(compact.length(), 160));
    }

    private enum InspectionKind { PLAIN_TEXT, STRUCTURED_HTML, UNSUPPORTED_MARKUP, UNOWNED_IMAGE }
    private record Inspection(InspectionKind kind) { }

    public enum HistoricalDecision {
        PLAIN_TEXT, RECOGNIZED_HTML, SANITIZED_HTML, ALREADY_NORMALIZED,
        NULL_CONTENT, AMBIGUOUS_MARKUP, UNOWNED_IMAGE
    }

    public record HistoricalNormalization(RichTextValue value, HistoricalDecision decision, boolean convertible) {
        static HistoricalNormalization converted(RichTextValue value, HistoricalDecision decision) {
            return new HistoricalNormalization(value, decision, true);
        }

        static HistoricalNormalization skipped(HistoricalDecision decision) {
            return new HistoricalNormalization(null, decision, false);
        }
    }
}
