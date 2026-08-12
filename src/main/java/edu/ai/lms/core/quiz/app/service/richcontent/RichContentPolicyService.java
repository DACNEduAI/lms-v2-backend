package edu.ai.lms.core.quiz.app.service.richcontent;

import java.util.List;
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
    private static final Pattern MATH_PATTERN = Pattern.compile(
            "data-type=\\\"(?:inline-math|block-math)\\\"[^>]*data-latex=\\\"([^\\\"]*)\\\"|"
                    + "data-latex=\\\"([^\\\"]*)\\\"[^>]*data-type=\\\"(?:inline-math|block-math)\\\"");
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
        var matcher = MATH_PATTERN.matcher(html);
        while (matcher.find()) {
            String latex = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            if (latex != null && !latex.isBlank()) return true;
        }
        return !TAG_PATTERN.matcher(html).replaceAll("").replace("&nbsp;", " ").isBlank();
    }

    public void validateLatex(String html) {
        var matcher = MATH_PATTERN.matcher(html == null ? "" : html);
        while (matcher.find()) {
            String latex = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            if (latex == null || latex.isBlank()) throw new RichContentValidationException("LaTeX source is required");
            if (latex.length() > MAX_LATEX_LENGTH) throw new RichContentValidationException("LaTeX source is too long");
            String lower = latex.toLowerCase();
            if (FORBIDDEN_LATEX_COMMANDS.stream().anyMatch(lower::contains)) {
                throw new RichContentValidationException("LaTeX contains a forbidden command");
            }
        }
    }
}
