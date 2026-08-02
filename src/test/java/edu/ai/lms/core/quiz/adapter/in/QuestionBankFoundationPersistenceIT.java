package edu.ai.lms.core.quiz.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.QuestionGenerationProvenance;
import edu.ai.lms.core.quiz.app.domain.QuestionReviewStatus;
import edu.ai.lms.core.quiz.app.domain.QuestionSourceType;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.domain.QuizLifecycleStatus;
import edu.ai.lms.core.quiz.app.domain.QuizSourceType;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankAnswer;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItem;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankItemStatus;
import edu.ai.lms.core.quiz.app.domain.questionbank.QuestionBankSourceType;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionGenerationProvenancePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionBankItemPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionGenerationProvenancePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizPort;

class QuestionBankFoundationPersistenceIT extends IntegrationTestConfiguration {
    @Autowired
    private SaveQuestionBankItemPort saveBankItemPort;

    @Autowired
    private LoadQuestionBankItemPort loadBankItemPort;

    @Autowired
    private SaveQuizPort saveQuizPort;

    @Autowired
    private SaveQuestionPort saveQuestionPort;

    @Autowired
    private SaveQuestionGenerationProvenancePort saveProvenancePort;

    @Autowired
    private LoadQuestionGenerationProvenancePort loadProvenancePort;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsLoadsAndOptimisticallyReplacesQuestionBankDraft() {
        FoundationIds ids = createFoundationData();
        QuestionBankItem saved = saveBankItemPort.create(bankItem(ids, "Original question"));

        assertThat(saved.getStatus()).isEqualTo(QuestionBankItemStatus.DRAFT);
        assertThat(saved.getSourceType()).isEqualTo(QuestionBankSourceType.MANUAL);
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getAnswers()).extracting(QuestionBankAnswer::getContent)
                .containsExactly("Option A", "Option B");
        assertThat(saved.getLearningOutcomeIds()).containsExactly(ids.learningOutcomeId());

        QuestionBankItem replacement = bankItem(ids, "Replacement question");
        replacement.setId(saved.getId());
        replacement.setAnswers(List.of(
                answer("Replacement A", true),
                answer("Replacement B", false),
                answer("Replacement C", false)));

        assertThat(saveBankItemPort.replaceDraft(replacement, 0)).isTrue();

        QuestionBankItem reloaded = loadBankItemPort
                .findByCourseIdAndId(ids.courseId(), saved.getId())
                .orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1);
        assertThat(reloaded.getContent()).isEqualTo("Replacement question");
        assertThat(reloaded.getAnswers()).extracting(QuestionBankAnswer::getOrderIndex)
                .containsExactly(0, 1, 2);

        QuestionBankItem stale = bankItem(ids, "Stale overwrite");
        stale.setId(saved.getId());
        assertThat(saveBankItemPort.replaceDraft(stale, 0)).isFalse();
        assertThat(loadBankItemPort.findByCourseIdAndId(ids.courseId(), saved.getId()).orElseThrow().getContent())
                .isEqualTo("Replacement question");
    }

    @Test
    void roundTripsQuizQuestionLifecycleAndGenerationProvenance() {
        FoundationIds ids = createFoundationData();
        Quiz quiz = saveQuizPort.save(Quiz.builder()
                .quizTitle("AI draft")
                .courseId(ids.courseId())
                .teacherId(ids.userId())
                .unlimited(true)
                .display(false)
                .lifecycleStatus(QuizLifecycleStatus.REVIEW_PENDING)
                .sourceType(QuizSourceType.AI_GENERATED)
                .build());
        Question question = saveQuestionPort.save(Question.builder()
                .quizId(quiz.getId())
                .content("Generated question")
                .type(Question.QuestionType.MULTIPLE_CHOICE)
                .reviewStatus(QuestionReviewStatus.PENDING_REVIEW)
                .sourceType(QuestionSourceType.AI_GENERATED)
                .build());

        UUID matrixId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO edu.exam_matrices (id, course_id, title) VALUES (?, ?, ?)",
                matrixId,
                ids.courseId(),
                "Foundation matrix");
        jdbc.update(
                """
                INSERT INTO edu.quiz_generation_jobs
                    (id, course_id, exam_matrix_id, quiz_id, requested_by, requested_role,
                     request_snapshot, payload_hash, status)
                VALUES (?, ?, ?, ?, ?, 'TEACHER', CAST(? AS jsonb), ?, 'COMPLETED')
                """,
                jobId,
                ids.courseId(),
                matrixId,
                quiz.getId(),
                ids.userId(),
                "{\"contractVersion\":\"3\"}",
                "foundation-hash");

        saveProvenancePort.save(QuestionGenerationProvenance.builder()
                .questionId(question.getId())
                .generationJobId(jobId)
                .questionType(Question.QuestionType.MULTIPLE_CHOICE)
                .allocationSnapshot("{\"questionCount\":1}")
                .build());

        QuestionGenerationProvenance reloaded = loadProvenancePort
                .findByQuestionId(question.getId())
                .orElseThrow();
        assertThat(reloaded.getGenerationJobId()).isEqualTo(jobId);
        assertThat(reloaded.getQuestionType()).isEqualTo(Question.QuestionType.MULTIPLE_CHOICE);
        assertThat(reloaded.getAllocationSnapshot()).contains("questionCount");
        assertThat(jdbc.queryForObject(
                "SELECT lifecycle_status FROM edu.quizzes WHERE id = ?",
                String.class,
                quiz.getId())).isEqualTo("REVIEW_PENDING");
        assertThat(jdbc.queryForObject(
                "SELECT review_status FROM edu.questions WHERE id = ?",
                String.class,
                question.getId())).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void deletingChapterClearsClassificationWithoutDeletingBankItem() {
        FoundationIds ids = createFoundationData();
        QuestionBankItem saved = saveBankItemPort.create(bankItem(ids, "Historical bank question"));

        jdbc.update("DELETE FROM edu.chapters WHERE chapter_id = ?", ids.chapterId());

        QuestionBankItem reloaded = loadBankItemPort
                .findByCourseIdAndId(ids.courseId(), saved.getId())
                .orElseThrow();
        assertThat(reloaded.getChapterId()).isNull();
        assertThat(reloaded.getContent()).isEqualTo("Historical bank question");
    }

    private QuestionBankItem bankItem(FoundationIds ids, String content) {
        return QuestionBankItem.builder()
                .courseId(ids.courseId())
                .chapterId(ids.chapterId())
                .content(content)
                .questionType(Question.QuestionType.MULTIPLE_CHOICE)
                .createdBy(ids.userId())
                .answers(List.of(answer("Option A", true), answer("Option B", false)))
                .learningOutcomeIds(new LinkedHashSet<>(List.of(ids.learningOutcomeId())))
                .build();
    }

    private QuestionBankAnswer answer(String content, boolean correct) {
        return QuestionBankAnswer.builder().content(content).correct(correct).build();
    }

    private FoundationIds createFoundationData() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID learningOutcomeId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO edu.users (id, email, password, role) VALUES (?, ?, 'password', 'TEACHER')",
                userId,
                userId + "@example.com");
        jdbc.update(
                "INSERT INTO edu.teachers (user_id, employee_id) VALUES (?, ?)",
                userId,
                "EMP-" + userId);
        jdbc.update(
                """
                INSERT INTO edu.courses (id, course_name, course_code, start_date, end_date)
                VALUES (?, 'Question Bank course', ?, NOW(), NOW() + INTERVAL '30 days')
                """,
                courseId,
                "QB-" + courseId);
        jdbc.update(
                "INSERT INTO edu.chapters (chapter_id, title, course_id) VALUES (?, 'Chapter 1', ?)",
                chapterId,
                courseId);
        jdbc.update(
                "INSERT INTO edu.learning_outcomes (id, prefix_number, description, course_id) VALUES (?, 1, 'LO 1', ?)",
                learningOutcomeId,
                courseId);
        return new FoundationIds(userId, courseId, chapterId, learningOutcomeId);
    }

    private record FoundationIds(UUID userId, UUID courseId, UUID chapterId, UUID learningOutcomeId) {
    }
}
