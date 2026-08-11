package edu.ai.lms.core.quiz.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssessmentRepositoryUT {

    @Test
    void appendsCopyEveryTimeWithoutCounters() {
        assertThat(AssessmentRepository.copyName("CCCT2026")).isEqualTo("CCCT2026-Copy");
        assertThat(AssessmentRepository.copyName("CCCT2026-Copy")).isEqualTo("CCCT2026-Copy-Copy");
    }
}
