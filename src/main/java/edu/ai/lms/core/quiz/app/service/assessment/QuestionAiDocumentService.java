package edu.ai.lms.core.quiz.app.service.assessment;

import edu.ai.lms.core.quiz.app.domain.assessment.QuestionAiModels.Anchor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DOM-only transformations: AI never receives or moves SVG/LaTeX elements. */
@Component
public class QuestionAiDocumentService {
    public record Projection(String content, List<String> tokens, Map<String, Element> protectedElements) {}

    public Projection protectArtifacts(String html) {
        Document document = Jsoup.parseBodyFragment(html == null ? "" : html);
        Map<String, Element> elements = new LinkedHashMap<>();
        int index = 0;
        for (Element element : new ArrayList<>(document.body().select("[data-latex], img"))) {
            String id = element.attr("data-ai-artifact-id");
            String identity = id.isBlank() ? "legacy-" + index : id;
            String token = "[[AI_ARTIFACT:" + identity + ":" + index++ + "]]";
            elements.put(token, element.clone());
            element.replaceWith(new TextNode(token));
        }
        return new Projection(document.body().html(), List.copyOf(elements.keySet()), Map.copyOf(elements));
    }

    public String restoreArtifacts(String html, Projection projection) {
        Document document = Jsoup.parseBodyFragment(html == null ? "" : html);
        for (Map.Entry<String, Element> entry : projection.protectedElements().entrySet()) {
            if (!replaceToken(document.body(), entry.getKey(), entry.getValue().clone())) {
                throw new IllegalArgumentException("Text specialist removed or changed an artifact placeholder");
            }
        }
        for (String token : projection.tokens()) {
            if (document.body().text().contains(token)) {
                throw new IllegalArgumentException("Text specialist duplicated an artifact placeholder");
            }
        }
        return document.body().html();
    }

    public String insertLatex(String html, UUID artifactId, String source, String mode, Anchor anchor) {
        Element node = new Element("BLOCK".equals(mode) ? "div" : "span")
                .attr("data-type", "BLOCK".equals(mode) ? "block-math" : "inline-math")
                .attr("data-latex", source)
                .attr("data-ai-artifact-id", artifactId.toString())
                .attr("data-ai-artifact-type", "LATEX");
        return insert(html, node, anchor);
    }

    public String insertSvg(String html, UUID artifactId, String altText, String sourceUrl, Anchor anchor) {
        Element node = new Element("img").attr("src", sourceUrl).attr("alt", altText)
                .attr("data-ai-artifact-id", artifactId.toString()).attr("data-ai-artifact-type", "SVG")
                .attr("data-image-width", "50").attr("style", "width: 50%; height: auto;");
        return insert(html, node, anchor);
    }

    /** Insert new Quiz3 AI SVGs as an inline Tiptap image so teachers can move and resize them. */
    public String insertInlineSvg(String html, UUID artifactId, String altText, String sourceUrl, Anchor anchor) {
        Element node = new Element("img").attr("src", sourceUrl).attr("alt", altText)
                .attr("data-ai-artifact-id", artifactId.toString()).attr("data-ai-artifact-type", "SVG")
                .attr("data-image-layout", "inline");
        return insert(html, node, anchor);
    }

    public String updateLatex(String html, UUID artifactId, String source, String mode) {
        Document document = parse(html);
        Element node = owned(document, artifactId, "LATEX");
        node.tagName("BLOCK".equals(mode) ? "div" : "span");
        node.attr("data-type", "BLOCK".equals(mode) ? "block-math" : "inline-math");
        node.attr("data-latex", source);
        return document.body().html();
    }

    public String tagLegacyLatex(String html, String source, String mode, UUID artifactId) {
        Document document = parse(html);
        String tag = "BLOCK".equals(mode) ? "div" : "span";
        Element match = document.select(tag + "[data-latex]").stream()
                .filter(node -> !node.hasAttr("data-ai-artifact-id") && source.equals(node.attr("data-latex")))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Legacy LaTeX formula was not found at the selected artifact"));
        match.attr("data-ai-artifact-id", artifactId.toString()).attr("data-ai-artifact-type", "LATEX");
        return document.body().html();
    }

    public String deleteArtifact(String html, UUID artifactId, String type) {
        Document document = parse(html);
        owned(document, artifactId, type).remove();
        return document.body().html();
    }

    public String deleteLegacyLatex(String html, String source, String mode) {
        Document document = parse(html);
        String tag = "BLOCK".equals(mode) ? "div" : "span";
        Element match = document.select(tag + "[data-latex]").stream()
                .filter(node -> !node.hasAttr("data-ai-artifact-id") && source.equals(node.attr("data-latex")))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Legacy LaTeX formula was not found"));
        match.remove();
        return document.body().html();
    }

    public String tagLegacySvg(String html, String sourceUrl, UUID artifactId) {
        Document document = parse(html);
        Element match = legacyImage(document, sourceUrl);
        match.attr("data-ai-artifact-id", artifactId.toString()).attr("data-ai-artifact-type", "SVG");
        return document.body().html();
    }

    public String deleteLegacySvg(String html, String sourceUrl) {
        Document document = parse(html);
        legacyImage(document, sourceUrl).remove();
        return document.body().html();
    }

    private Element legacyImage(Document document, String sourceUrl) {
        return document.select("img[src]").stream()
                .filter(node -> !node.hasAttr("data-ai-artifact-id") && sourceUrl.equals(node.attr("src")))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Legacy SVG image was not found"));
    }

    private String insert(String html, Element node, Anchor anchor) {
        Document document = parse(html);
        Anchor resolved = anchor == null ? new Anchor("END", null, null) : anchor;
        if ("CURSOR".equals(resolved.kind()) && resolved.offset() != null
                && insertAtTextOffset(document.body(), resolved.offset(), node)) {
            return document.body().html();
        }
        document.body().appendChild(node);
        return document.body().html();
    }

    private boolean insertAtTextOffset(Element body, int offset, Element insertion) {
        int remaining = offset;
        for (TextNode text : allTextNodes(body)) {
            int length = text.getWholeText().length();
            if (remaining <= length) {
                if (remaining == 0) text.before(insertion);
                else if (remaining == length) text.after(insertion);
                else text.splitText(remaining).before(insertion);
                return true;
            }
            remaining -= length;
        }
        return false;
    }

    private List<TextNode> allTextNodes(Node root) {
        List<TextNode> result = new ArrayList<>();
        collectTextNodes(root, result);
        return result;
    }

    private void collectTextNodes(Node node, List<TextNode> result) {
        if (node instanceof TextNode text) result.add(text);
        else for (Node child : node.childNodes()) collectTextNodes(child, result);
    }

    private Element owned(Document document, UUID artifactId, String type) {
        Element node = document.selectFirst("[data-ai-artifact-id='" + artifactId + "']");
        if (node == null || !type.equals(node.attr("data-ai-artifact-type"))) {
            throw new IllegalArgumentException("AI artifact is not present in this question draft");
        }
        return node;
    }

    private boolean replaceToken(Element root, String token, Node replacement) {
        for (TextNode text : new ArrayList<>(root.textNodes())) {
            String whole = text.getWholeText();
            int index = whole.indexOf(token);
            if (index < 0) continue;
            if (index > 0) text.before(new TextNode(whole.substring(0, index)));
            text.before(replacement);
            int afterIndex = index + token.length();
            if (afterIndex < whole.length()) text.before(new TextNode(whole.substring(afterIndex)));
            text.remove();
            return true;
        }
        for (Element child : root.children()) if (replaceToken(child, token, replacement)) return true;
        return false;
    }

    private Document parse(String html) { return Jsoup.parseBodyFragment(html == null ? "" : html); }
}
