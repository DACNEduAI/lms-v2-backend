package edu.ai.lms.core.course.app.service.ccct;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;
import edu.ai.lms.core.course.app.domain.CcctProfile;

@Component
class CcctMarkdownSerializer {

    String serialize(CcctProfile profile) {
        List<String> lines = new ArrayList<>();
        lines.add("# " + profile.getName().trim());
        profile.getChapters().forEach(chapter -> {
            lines.add("");
            lines.add("## " + chapter.getTitle().trim());
            chapter.getTopics().forEach(topic -> {
                lines.add("");
                lines.add("### " + topic.getTitle().trim());
                topic.getItems().forEach(item -> {
                    lines.add("- " + item.getContent().trim() + " — " + label(item.getCoverageStatus()));
                    if (item.getNote() != null && !item.getNote().isBlank()) {
                        lines.add("  - Note: " + item.getNote().trim());
                    }
                });
            });
        });
        return String.join("\n", lines);
    }

    private String label(CcctCoverageStatus status) {
        return switch (status) {
            case LEARN_AND_EXAM -> "Learn and exam";
            case LEARN_ONLY -> "Learn, no exam";
            case SKIP -> "Do not learn";
        };
    }
}
