package edu.ai.lms.core.quiz.adapter.out.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.CssSchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ai.lms.core.quiz.app.port.out.SanitizeRichContentPort;

/**
 * The browser may be bypassed. This is the final persistence boundary for
 * Tiptap HTML. The policy intentionally accepts only nodes produced by the
 * editor extension factory.
 */
@Component
public class OwaspRichContentSanitizer implements SanitizeRichContentPort {
    private static final Pattern LINK_TAG = Pattern.compile("<a\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern REL_ATTRIBUTE = Pattern.compile("\\srel=\\\"[^\\\"]*\\\"", Pattern.CASE_INSENSITIVE);

    private final PolicyFactory policy = new HtmlPolicyBuilder()
            .allowElements("p", "br", "h1", "h2", "h3", "h4", "h5", "h6", "strong", "em", "u", "s",
                    "code", "pre", "blockquote", "hr", "ul", "ol", "li", "input", "table", "thead",
                    "tbody", "tr", "th", "td", "a", "img", "span", "div", "mark")
            .allowAttributes("class").onElements("span", "div", "p", "table", "thead", "tbody", "tr", "th", "td",
                    "ul", "ol", "li", "blockquote", "pre", "code")
            .allowAttributes("style").onElements("span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "img")
            .allowAttributes("href", "target", "rel").onElements("a")
            .allowAttributes("src", "alt", "title", "width", "height", "data-image-width", "data-image-layout").onElements("img")
            .allowAttributes("colspan", "rowspan", "colwidth").onElements("th", "td")
            .allowAttributes("type", "checked", "data-checked").onElements("input", "li")
            .allowAttributes("data-type", "data-latex", "data-ai-artifact-id", "data-ai-artifact-type")
                .onElements("span", "div")
            .allowAttributes("data-ai-artifact-id", "data-ai-artifact-type").onElements("img")
            .allowAttributes("data-color").onElements("mark")
            .allowUrlProtocols("http", "https", "mailto")
            .allowStyling(CssSchema.withProperties(List.of(
                    "color", "background-color", "font-family", "font-size", "line-height", "text-align", "width", "height")))
            .toFactory();

    @Override
    public String sanitize(String html) {
        String sanitized = policy.sanitize(html == null ? "" : html);
        Matcher matcher = LINK_TAG.matcher(sanitized);
        StringBuffer normalized = new StringBuffer();
        while (matcher.find()) {
            String link = REL_ATTRIBUTE.matcher(matcher.group()).replaceAll("");
            link = link.substring(0, link.length() - 1) + " rel=\"noopener noreferrer\">";
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(link));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }
}
