package edu.ai.lms;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackageStructureTest {

    private static final Pattern MODULE_IN_PATH = Pattern.compile("/core/([^/]+)/");
    private static final Pattern CORE_IMPORT = Pattern.compile(
            "^import edu\\.ai\\.lms\\.core\\.([^.]+)\\.", Pattern.MULTILINE);
    private static final Pattern QUALIFIED_TABLE = Pattern.compile(
            "\\bedu\\.([a-z_]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> RETIRED_TABLES = List.of(
            "assessments", "assessment_variants", "quiz_generation_jobs",
            "assessment_generation_batches", "assessment_generation_batch_items",
            "assessment_webhook_events", "question_drafts", "question_draft_answers",
            "question_draft_learning_outcomes", "question_bank_questions",
            "question_bank_answers", "question_bank_question_learning_outcomes",
            "question_bank_allocations", "question_ai_working_drafts", "question_ai_jobs",
            "question_ai_job_steps", "question_ai_artifacts", "question_ai_artifact_revisions",
            "question_ai_change_sets", "rich_content_assets", "question_generation_provenance",
            "question_images", "question_image_generation_jobs", "exam_matrices",
            "exam_matrix_rows", "exam_matrix_row_learning_outcomes", "custom_learning_levels",
            "custom_learning_level_learning_outcomes", "exam_matrix_row_learning_level_counts");

    @Test
    void verifyQuiz3MigrationBoundary() throws IOException {
        Path sourceRoot = Path.of("src/main/java/edu/ai/lms/core");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            if (Files.readString(path).contains("edu.ai.lms.core.quiz.")) {
                                violations.add(path.toString());
                            }
                        } catch (IOException exception) {
                            throw new IllegalStateException("Could not inspect " + path, exception);
                        }
                    });
        }
        assertTrue(violations.isEmpty(), () -> "References to retired core.quiz:\n" + String.join("\n", violations));
    }

    @Test
    void applicationAndDomainLayersDoNotImportOtherModules() throws IOException {
        Path sourceRoot = Path.of("src/main/java/edu/ai/lms/core");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/core/quiz3/"))
                    .forEach(path -> inspectImports(path, violations));
        }

        assertTrue(violations.isEmpty(),
                () -> "Quiz3 application/domain code still imports retired core.quiz:\n"
                        + String.join("\n", violations));
    }

    @Test
    void repositoriesDoNotQueryTablesOwnedByOtherModules() throws IOException {
        Path sourceRoot = Path.of("src/main/java/edu/ai/lms/core");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().contains("/adapter/out/persistence/"))
                    .filter(path -> path.toString().endsWith(".java")
                            || path.toString().endsWith(".xml"))
                    .forEach(path -> inspectOwnedTables(path, violations));
        }

        assertTrue(violations.isEmpty(),
                () -> "Repositories still access retired Quiz2 or Matrix 1 tables:\n"
                        + String.join("\n", violations));
    }

    private static void inspectImports(Path path, List<String> violations) {
        try {
            String normalizedPath = path.toString().replace('\\', '/');
            Matcher sourceMatcher = MODULE_IN_PATH.matcher(normalizedPath);
            if (!sourceMatcher.find()) {
                return;
            }
            Matcher imports = CORE_IMPORT.matcher(Files.readString(path));
            while (imports.find()) {
                String targetModule = imports.group(1);
                if ("quiz".equals(targetModule)) {
                    violations.add(normalizedPath + " -> core.quiz");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect " + path, exception);
        }
    }

    private static void inspectOwnedTables(Path path, List<String> violations) {
        try {
            String normalizedPath = path.toString().replace('\\', '/');
            Matcher sourceMatcher = MODULE_IN_PATH.matcher(normalizedPath);
            if (!sourceMatcher.find()) {
                return;
            }
            Matcher tables = QUALIFIED_TABLE.matcher(Files.readString(path));
            while (tables.find()) {
                String table = tables.group(1).toLowerCase();
                if (RETIRED_TABLES.contains(table)) {
                    violations.add(normalizedPath + " -> edu." + table);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect " + path, exception);
        }
    }
}
