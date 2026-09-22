package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.Quiz3Models;
import edu.ai.lms.core.quiz3.adapter.out.persistence.Quiz3RichContentAssetRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class Quiz3QBank2RegenerationContractUT {
    @Test
    void acceptsZeroOrOneExampleId() {
        UUID one = UUID.randomUUID();
        assertEquals(List.of(), Quiz3QBank2RegenerationService.normalizeExampleIds(null));
        assertDoesNotThrow(() -> Quiz3QBank2RegenerationService.normalizeExampleIds(List.of(one)));
    }

    @Test
    void rejectsDuplicateOrMoreThanOneExampleIdBeforeAnyAiCall() {
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        assertThrows(ResponseStatusException.class, () -> Quiz3QBank2RegenerationService.normalizeExampleIds(List.of(one, one)));
        assertThrows(ResponseStatusException.class, () -> Quiz3QBank2RegenerationService.normalizeExampleIds(List.of(one, two)));
    }
}
