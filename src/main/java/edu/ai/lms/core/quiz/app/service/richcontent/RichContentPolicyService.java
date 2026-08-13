package edu.ai.lms.core.quiz.app.service.richcontent;

import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
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

    private static final String MATH_NODE_SELECTOR =
            "span[data-type], span[data-latex], div[data-type], div[data-latex]";
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
        Element body = parseBody(value.content());
        if (body.selectFirst("img[src]") != null) return true;
        if (hasMathNode(body, false)) return true;
        return !body.text().isBlank();
    }

    public void validateLatex(String html) {
        hasMathNode(parseBody(html), true);
    }

    /**
     * KaTeX is the canonical syntax validator in the teacher editor. The
     * server owns the durable safety boundary: only genuine Tiptap math nodes,
     * bounded source and structurally balanced groups may be persisted.
     */
    private boolean hasMathNode(Element body, boolean validate) {
        boolean found = false;
        for (Element node : body.select(MATH_NODE_SELECTOR)) {
            String type = node.hasAttr("data-type") ? node.attr("data-type") : null;
            String latex = node.hasAttr("data-latex") ? node.attr("data-latex") : null;
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

    private Element parseBody(String html) {
        return Jsoup.parseBodyFragment(html == null ? "" : html).body();
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
