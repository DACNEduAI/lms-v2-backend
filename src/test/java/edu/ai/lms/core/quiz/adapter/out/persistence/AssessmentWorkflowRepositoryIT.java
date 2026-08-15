package edu.ai.lms.core.quiz.adapter.out.persistence;

import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.AssessmentView;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlan;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.BankPlanCell;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewPage;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.DraftReviewSearch;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.LifecycleStatus;
import static edu.ai.lms.core.quiz.app.domain.assessment.AssessmentModels.VariantView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.quiz.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz.app.exception.QuestionVersionConflictException;

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
            assertThat(question.metadata().matrixRow()).isEqualTo("Hàng Matrix 1");
            assertThat(question.metadata().topic()).isEqualTo("Stack operations");
            assertThat(question.metadata().customLearningLevelCode()).isEqualTo("REMEMBER");
            assertThat(question.metadata().learningOutcomes()).extracting(outcome -> outcome.id())
                    .containsExactly(fixture.learningOutcomeId());
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
    void createsCodeRunnerVariantWithAReviewPendingContractSnapshot() {
        Fixture fixture = createFixture();
        jdbc.update("DELETE FROM edu.question_bank_answers WHERE question_bank_question_id = ?",
                fixture.bankQuestionId());
        jdbc.update("""
                UPDATE edu.question_bank_questions
                SET question_type = 'CODE_RUNNER', code_runner_contract = CAST(? AS jsonb)
                WHERE id = ?
                """, verifiedCodeRunnerContract(), fixture.bankQuestionId());
        BankPlanCell original = fixture.plan().cells().get(0);
        BankPlanCell codeRunnerCell = new BankPlanCell(
                original.matrixRowId(), original.customLearningLevelId(), original.chapterId(),
                original.chapter(), original.topic(), original.levelCode(), original.levelName(),
                "CODE_RUNNER", original.learningOutcomeIds(), original.required(), original.validated(),
                original.recentlyUsed(), original.reserved(), original.available(), original.missing());
        BankPlan codeRunnerPlan = new BankPlan(
                fixture.plan().assessmentId(), fixture.plan().examMatrixId(), fixture.plan().allowRecentlyUsedQuestions(),
                fixture.plan().required(), fixture.plan().validated(), fixture.plan().recentlyUsed(),
                fixture.plan().reserved(), fixture.plan().available(), fixture.plan().missing(), List.of(codeRunnerCell));

        VariantView variant = workflow.createVariants(fixture.assessment(), codeRunnerPlan).get(0);

        assertThat(variant.questions()).singleElement().satisfies(question -> {
            assertThat(question.type()).isEqualTo("CODE_RUNNER");
            assertThat(question.answers()).isEmpty();
        });
        assertThat(jdbc.queryForObject("""
                SELECT c.contract_status
                FROM edu.code_runner_configs c
                JOIN edu.questions q ON q.id = c.question_id
                WHERE q.quiz_id = ?
                """, String.class, variant.quizId())).isEqualTo("READY_FOR_REVIEW");
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

    @Test
    void savesVariantQuestionWithRequiredVersionAndRejectsStaleWrites() {
        Fixture fixture = createFixture();
        VariantView variant = workflow.createVariants(fixture.assessment(), fixture.plan()).getFirst();
        var question = variant.questions().getFirst();
        List<AssessmentWorkflowRepository.AnswerInput> answers = question.answers().stream()
                .map(answer -> new AssessmentWorkflowRepository.AnswerInput(answer.content(), answer.contentFormat(),
                        answer.correct()))
                .toList();

        workflow.updateVariantQuestion(fixture.assessment().id(), variant.id(), question.id(), question.version(),
                "Updated question content", RichContentFormat.PLAIN_TEXT, null, RichContentFormat.PLAIN_TEXT, answers);

        assertThat(jdbc.queryForObject("SELECT content FROM edu.questions WHERE id = ?", String.class, question.id()))
                .isEqualTo("Updated question content");
        assertThat(jdbc.queryForObject("SELECT version FROM edu.questions WHERE id = ?", Long.class, question.id()))
                .isEqualTo(question.version() + 1);
        assertThatThrownBy(() -> workflow.updateVariantQuestion(fixture.assessment().id(), variant.id(), question.id(),
                question.version(), "Stale update", RichContentFormat.PLAIN_TEXT, null,
                RichContentFormat.PLAIN_TEXT, answers))
                .isInstanceOf(QuestionVersionConflictException.class);
    }

    private Fixture createFixture() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID matrixId = UUID.randomUUID();
        UUID matrixRowId = UUID.randomUUID();
        UUID customLearningLevelId = UUID.randomUUID();
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
        jdbc.update("INSERT INTO edu.exam_matrices (id, course_id, title) VALUES (?, ?, 'Assessment matrix')",
                matrixId, courseId);
        jdbc.update("""
                INSERT INTO edu.exam_matrix_rows (id, matrix_id, topic, order_index)
                VALUES (?, ?, 'Stack operations', 0)
                """, matrixRowId, matrixId);
        jdbc.update("""
                INSERT INTO edu.custom_learning_levels (id, matrix_id, code, name, sort_order)
                VALUES (?, ?, 'REMEMBER', 'Remember', 1)
                """, customLearningLevelId, matrixId);
        jdbc.update("""
                INSERT INTO edu.question_bank_questions
                    (id, origin_draft_id, assessment_id, course_id, cognitive_level_id, matrix_row_id, custom_learning_level_id,
                     content, question_type, created_by, validated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'Which option is correct?', 'MULTIPLE_CHOICE', ?, ?)
                """, bankQuestionId, UUID.randomUUID(), assessmentId, courseId, cognitiveLevelId, matrixRowId,
                customLearningLevelId, userId, userId);
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
                null, null, matrixId, null, true, 0, userId, null, null, now, now);
        BankPlanCell cell = new BankPlanCell(matrixRowId, customLearningLevelId, null, null, "Stack operations",
                "REMEMBER", "Remember", List.of(learningOutcomeId), 1, 1, 0, 0, 1, 0);
        BankPlan plan = new BankPlan(assessmentId, null, false, 1, 1, 0, 0, 1, 0, List.of(cell));
        return new Fixture(assessment, plan, bankQuestionId, learningOutcomeId);
    }

    private String verifiedCodeRunnerContract() {
        return """
                {
                  "schema_version":"2.0.0",
                  "contract_id":"is_even",
                  "contract_version":"1.0.0",
                  "status":"VERIFIED_EXECUTION_GOLD",
                  "title":"Check even number",
                  "statement":"Return true when value is even.",
                  "runtime":{
                    "execution_profile":{"id":"dsa_cpp17","version":"1.0.0"},
                    "archetype":{"id":"scalar_function","version":"1.0.0"},
                    "comparator":{"id":"exact_typed","version":"1.0.0"},
                    "entry_point":"isEven",
                    "signature":{
                      "return_fixture":{"id":"boolean","version":"1.0.0"},
                      "arguments":[{"name":"value","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}}]
                    },
                    "starter_code":"bool isEven(int value) { return false; }",
                    "public_tests":[{
                      "id":"public_1","category":"example","visibility":"PUBLIC",
                      "arguments":{"value":2},"expected_output":true,"points":0
                    }],
                    "banned_api_patterns":[]
                  },
                  "private_grading":{
                    "reference_solution":"bool isEven(int value) { return value % 2 == 0; }",
                    "hidden_tests":[{
                      "id":"hidden_1","category":"hidden","visibility":"HIDDEN",
                      "arguments":{"value":3},"expected_output":false,"points":1
                    }]
                  }
                }
                """;
    }

    private record Fixture(AssessmentView assessment, BankPlan plan, UUID bankQuestionId, UUID learningOutcomeId) {
    }
}
