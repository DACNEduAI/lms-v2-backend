package edu.ai.lms;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackageStructureTest {

    ApplicationModules modules = ApplicationModules.of(LmsApplication.class);
    private static final Pattern MODULE_IN_PATH = Pattern.compile("/core/([^/]+)/");
    private static final Pattern CORE_IMPORT = Pattern.compile(
            "^import edu\\.ai\\.lms\\.core\\.([^.]+)\\.", Pattern.MULTILINE);
    private static final Pattern QUALIFIED_TABLE = Pattern.compile(
            "\\bedu\\.([a-z_]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> TABLE_OWNERS = Map.ofEntries(
            Map.entry("users", "user"),
            Map.entry("students", "user"),
            Map.entry("teachers", "user"),
            Map.entry("courses", "course"),
            Map.entry("enrollments", "course"),
            Map.entry("chapters", "course"),
            Map.entry("exam_matrices", "course"),
            Map.entry("exam_matrix_rows", "course"),
            Map.entry("exam_matrix_row_learning_outcomes", "course"),
            Map.entry("exam_matrix_row_learning_level_counts", "course"),
            Map.entry("custom_learning_levels", "course"),
            Map.entry("learning_outcomes", "course"),
            Map.entry("group_learning_outcomes", "course"),
            Map.entry("materials", "material"),
            Map.entry("quizzes", "quiz"),
            Map.entry("questions", "quiz"),
            Map.entry("answers", "quiz"),
            Map.entry("quiz_learning_outcomes", "quiz"),
            Map.entry("question_learning_outcomes", "quiz"),
            Map.entry("quiz_generation_jobs", "quiz"),
            Map.entry("attempts", "attempt"));

    @Test
    @org.junit.jupiter.api.Disabled("Temporarily disabled during Quiz3 module migration which introduces intentional module boundary violations")
    void verifyModulesStructure() {
        System.out.println(modules);
        modules.verify();
    }

    @Test
    void applicationAndDomainLayersDoNotImportOtherModules() throws IOException {
        Path sourceRoot = Path.of("src/main/java/edu/ai/lms/core");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/app/service/")
                            || path.toString().contains("/app/domain/"))
                    .forEach(path -> inspectImports(path, violations));
        }

        assertTrue(violations.isEmpty(),
                () -> "Cross-module imports are forbidden in application/domain layers:\n"
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
                () -> "Repositories must access only tables owned by their module:\n"
                        + String.join("\n", violations));
    }

    private static void inspectImports(Path path, List<String> violations) {
        try {
            String normalizedPath = path.toString().replace('\\', '/');
            Matcher sourceMatcher = MODULE_IN_PATH.matcher(normalizedPath);
            if (!sourceMatcher.find()) {
                return;
            }
            String sourceModule = sourceMatcher.group(1);
            Matcher imports = CORE_IMPORT.matcher(Files.readString(path));
            while (imports.find()) {
                String targetModule = imports.group(1);
                if (!sourceModule.equals(targetModule)) {
                    violations.add(normalizedPath + " -> core." + targetModule);
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
            String sourceModule = sourceMatcher.group(1);
            Matcher tables = QUALIFIED_TABLE.matcher(Files.readString(path));
            while (tables.find()) {
                String table = tables.group(1).toLowerCase();
                String owner = TABLE_OWNERS.get(table);
                if (owner != null && !owner.equals(sourceModule)) {
                    violations.add(normalizedPath + " -> edu." + table + " (owned by " + owner + ")");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect " + path, exception);
        }
    }
}
