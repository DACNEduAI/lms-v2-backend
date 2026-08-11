package edu.ai.lms.core.quiz.adapter.out.persistence;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlan;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlanCell;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewPage;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewSearch;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.LifecycleStatus;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.config.IntegrationTestConfiguration;

class AssessmentWorkflowRepositoryIT extends IntegrationTestConfiguration {
    @Autowired
    private AssessmentWorkflowRepository workflow;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsVariantByCopyingBankQuestionTypeAnswersAndLearningOutcomes() {
        Fixture fixture = createFixture();

        List<VariantView> variants = workflow.createVariants(fixture.assessment(), fixture.plan());

        assertThat(variants).hasSize(1);
        VariantView variant = variants.get(0);
        assertThat(variant.quizId()).isNotNull();
        assertThat(variant.questions()).singleElement().satisfies(question -> {
            assertThat(question.sourceBankQuestionId()).isEqualTo(fixture.bankQuestionId());
            assertThat(question.type()).isEqualTo("MULTIPLE_CHOICE");
            assertThat(question.answers()).extracting(answer -> answer.content())
                    .containsExactlyInAnyOrder("Option A", "Option B", "Option C", "Option D");
            assertThat(question.learningOutcomeIds()).containsExactly(fixture.learningOutcomeId());
        });
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM edu.assessment_variants WHERE assessment_id = ?",
                Integer.class,
                fixture.assessment().id())).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT type FROM edu.questions WHERE quiz_id = ?",
                String.class,
                variant.quizId())).isEqualTo("MULTIPLE_CHOICE");
    }

    @Test
    void returnsUnfilteredReviewItemsWithoutMergingTheTableAliasAndOrderBy() {
        Fixture fixture = createFixture();

        DraftReviewPage page = workflow.reviewPage(fixture.assessment().id(),
                new DraftReviewSearch(null, null, null, null, null, null, null, 0, 25));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.bankQuestionId()).isEqualTo(fixture.bankQuestionId());
            assertThat(item.status()).isEqualTo("VALIDATED");
            assertThat(item.content()).isEqualTo("Which option is correct?");
        });
    }

    private Fixture createFixture() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID bankQuestionId = UUID.randomUUID();
        UUID learningOutcomeId = UUID.randomUUID();
        UUID cognitiveLevelId = jdbc.queryForObject(
                "SELECT id FROM edu.cognitive_levels WHERE code = 'REMEMBER'", UUID.class);

        jdbc.update("INSERT INTO edu.users (id, email, password, role) VALUES (?, ?, 'password', 'TEACHER')",
                userId, userId + "@example.com");
        jdbc.update("INSERT INTO edu.teachers (user_id, employee_id) VALUES (?, ?)", userId, "EMP-" + userId);
        jdbc.update("""
                INSERT INTO edu.courses (id, course_name, course_code, start_date, end_date)
                VALUES (?, 'Assessment workflow course', ?, NOW(), NOW() + INTERVAL '30 days')
                """, courseId, "AW-" + courseId);
        jdbc.update("INSERT INTO edu.assessments (id, course_id, title, created_by) VALUES (?, ?, 'Variant assessment', ?)",
                assessmentId, courseId, userId);
        jdbc.update("""
                INSERT INTO edu.learning_outcomes (id, prefix_number, description, course_id)
                VALUES (?, 1, 'Learning outcome', ?)
                """, learningOutcomeId, courseId);
        jdbc.update("""
                INSERT INTO edu.question_bank_questions
                    (id, origin_draft_id, assessment_id, course_id, cognitive_level_id, content, question_type, created_by, validated_by)
                VALUES (?, ?, ?, ?, ?, 'Which option is correct?', 'MULTIPLE_CHOICE', ?, ?)
                """, bankQuestionId, UUID.randomUUID(), assessmentId, courseId, cognitiveLevelId, userId, userId);
        jdbc.update("""
                INSERT INTO edu.question_bank_question_learning_outcomes
                    (question_bank_question_id, learning_outcome_id)
                VALUES (?, ?)
                """, bankQuestionId, learningOutcomeId);
        for (int index = 0; index < 4; index++) {
            jdbc.update("""
                    INSERT INTO edu.question_bank_answers
                        (question_bank_question_id, content, is_correct, order_index)
                    VALUES (?, ?, ?, ?)
                    """, bankQuestionId, "Option " + (char) ('A' + index), index == 0, index);
        }

        LocalDateTime now = LocalDateTime.now();
        AssessmentView assessment = new AssessmentView(assessmentId, courseId, "Variant assessment", null,
                true, null, null, null, null, null, 1, 4, false, LifecycleStatus.REVIEW_PENDING, 8,
                null, null, null, null, true, 0, userId, null, null, now, now);
        BankPlanCell cell = new BankPlanCell(UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                "REMEMBER", "Remember", List.of(learningOutcomeId), 1, 1, 0, 0, 1, 0);
        BankPlan plan = new BankPlan(assessmentId, null, false, 1, 1, 0, 0, 1, 0, List.of(cell));
        return new Fixture(assessment, plan, bankQuestionId, learningOutcomeId);
    }

    private record Fixture(AssessmentView assessment, BankPlan plan, UUID bankQuestionId, UUID learningOutcomeId) {
    }
}
