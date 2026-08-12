package edu.ai.lms.core.quiz.app.service.richcontent;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz.app.port.out.SanitizeRichContentPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RichContentPolicyService {
    public static final int MAX_PROMPT_HTML_LENGTH = 256 * 1024;
    public static final int MAX_ANSWER_HTML_LENGTH = 128 * 1024;
    public static final int MAX_LATEX_LENGTH = 8 * 1024;

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MATH_TAG_PATTERN = Pattern.compile("<(?:span|div)\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\b([a-zA-Z-]+)\\s*=\\s*(['\\\"])(.*?)\\2");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("<img\\b[^>]*\\bsrc=\\\"[^\\\"]+\\\"", Pattern.CASE_INSENSITIVE);
    private static final List<String> FORBIDDEN_LATEX_COMMANDS = List.of(
            "\\\\html", "\\\\href", "\\\\url", "\\\\includegraphics", "\\\\def", "\\\\gdef", "\\\\newcommand", "\\\\catcode");

    private final SanitizeRichContentPort sanitizer;

    public RichTextValue sanitize(RichTextValue value, int maxLength) {
        RichTextValue normalized = value == null ? RichTextValue.plainText("") : value;
        if (normalized.content().length() > maxLength) {
            throw new RichContentValidationException("Rich content exceeds the allowed size");
        }
        if (normalized.contentFormat() == RichContentFormat.PLAIN_TEXT) return normalized;
        String sanitized = sanitizer.sanitize(normalized.content());
        validateLatex(sanitized);
        return new RichTextValue(sanitized, RichContentFormat.TIPTAP_HTML);
    }

    public boolean hasMeaningfulContent(RichTextValue value) {
        if (value == null) return false;
        if (value.contentFormat() == RichContentFormat.PLAIN_TEXT) return !value.content().isBlank();
        String html = value.content();
        if (IMAGE_PATTERN.matcher(html).find()) return true;
        if (hasMathNode(html, false)) return true;
        return !TAG_PATTERN.matcher(html).replaceAll("").replace("&nbsp;", " ").isBlank();
    }

    public void validateLatex(String html) {
        hasMathNode(html == null ? "" : html, true);
    }

    /**
     * KaTeX is the canonical syntax validator in the teacher editor. The
     * server owns the durable safety boundary: only genuine Tiptap math nodes,
     * bounded source and structurally balanced groups may be persisted.
     */
    private boolean hasMathNode(String html, boolean validate) {
        boolean found = false;
        var tagMatcher = MATH_TAG_PATTERN.matcher(html);
        while (tagMatcher.find()) {
            String type = attribute(tagMatcher.group(1), "data-type");
            String latex = attribute(tagMatcher.group(1), "data-latex");
            if (type == null && latex == null) continue;
            if (!"inline-math".equals(type) && !"block-math".equals(type)) {
                throw new RichContentValidationException("LaTeX node type is invalid");
            }
            if (latex == null || latex.isBlank()) throw new RichContentValidationException("LaTeX source is required");
            found = true;
            if (!validate) continue;
            if (latex.length() > MAX_LATEX_LENGTH) throw new RichContentValidationException("LaTeX source is too long");
            String lower = latex.toLowerCase(Locale.ROOT);
            if (FORBIDDEN_LATEX_COMMANDS.stream().anyMatch(lower::contains)) {
                throw new RichContentValidationException("LaTeX contains a forbidden command");
            }
            if (!hasBalancedGroups(latex)) throw new RichContentValidationException("LaTeX groups are unbalanced");
        }
        return found;
    }

    private String attribute(String attributes, String name) {
        var matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) if (name.equalsIgnoreCase(matcher.group(1))) return matcher.group(3);
        return null;
    }

    private boolean hasBalancedGroups(String latex) {
        int depth = 0;
        boolean escaped = false;
        for (int index = 0; index < latex.length(); index++) {
            char value = latex.charAt(index);
            if (value == '\\') {
                escaped = !escaped;
                continue;
            }
            if (!escaped && value == '{') depth++;
            if (!escaped && value == '}' && --depth < 0) return false;
            escaped = false;
        }
        return depth == 0;
    }
}
