package edu.ai.lms.core.course.app.service.ccct;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

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
                    lines.add("- " + item.getContent().trim());
                });
            });
        });
        return String.join("\n", lines);
    }
}
