package edu.ai.lms.core.course.app.service.qbank2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class QuestionBank2ServiceValidationUT {
    @Test
    void formatsPersistentGroupCodes() {
        assertEquals("QBG-001", QuestionBank2Service.formatCode(1));
        assertEquals("QBG-012", QuestionBank2Service.formatCode(12));
        assertEquals("QBG-1000", QuestionBank2Service.formatCode(1000));
    }

    @Test
    void rejectsBlankGroupAndExampleValues() {
        assertThrows(ResponseStatusException.class, () -> QuestionBank2Service.validateGroup(null));
        assertThrows(ResponseStatusException.class, () -> QuestionBank2Service.validateGroup(new QuestionBank2Service.GroupInput(" ", "Description")));
        assertThrows(ResponseStatusException.class, () -> QuestionBank2Service.validateGroup(new QuestionBank2Service.GroupInput("Name", " ")));
        assertThrows(ResponseStatusException.class, () -> QuestionBank2Service.validateExample(new QuestionBank2Service.ExampleInput("\n")));
    }
}
