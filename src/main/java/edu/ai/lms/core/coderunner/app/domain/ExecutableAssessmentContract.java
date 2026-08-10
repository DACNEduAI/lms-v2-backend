package edu.ai.lms.core.coderunner.app.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Versioned, executable definition of a Code Runner question.
 * Private grading data must only be exposed through teacher-authorized APIs.
 */
public record ExecutableAssessmentContract(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("contract_id") String contractId,
        @JsonProperty("contract_version") String contractVersion,
        String status,
        String title,
        String statement,
        RuntimeContract runtime,
        @JsonProperty("private_grading") PrivateGrading privateGrading) {

    public ExecutableAssessmentContract withStatus(String newStatus) {
        return new ExecutableAssessmentContract(
                schemaVersion,
                contractId,
                contractVersion,
                newStatus,
                title,
                statement,
                runtime,
                privateGrading);
    }

    private static final Set<String> ACCEPTED_STATUSES = Set.of(
            "READY_FOR_REVIEW", "VERIFIED_EXECUTION_GOLD");
    private static final Set<String> FIXTURES = Set.of(
            "integer", "boolean", "string", "vector_int");
    private static final Set<String> ARCHETYPES = Set.of(
            "scalar_function", "array_function", "sorting_function");

    public void validateForPersistence() {
        require("2.0.0".equals(schemaVersion), "Unsupported executable contract schema");
        require(ACCEPTED_STATUSES.contains(status), "Unsupported executable contract status");
        require(notBlank(contractId) && contractId.matches("^[a-z][a-z0-9_]*$"), "Invalid contract id");
        require(notBlank(contractVersion) && contractVersion.matches("^[0-9]+\\.[0-9]+\\.[0-9]+$"),
                "Invalid contract version");
        require(notBlank(title) && notBlank(statement), "Title and statement are required");
        require(runtime != null && privateGrading != null, "Runtime and private_grading are required");
        require(runtime.signature() != null && runtime.signature().arguments() != null,
                "Function signature is required");
        require(notBlank(runtime.entryPoint())
                        && runtime.entryPoint().matches("^[A-Za-z_][A-Za-z0-9_]*$"),
                "Invalid entry point");
        require(runtime.publicTests() != null && !runtime.publicTests().isEmpty(),
                "At least one public test is required");
        require(runtime.bannedApiPatterns() != null, "banned_api_patterns is required");
        require(privateGrading.hiddenTests() != null && !privateGrading.hiddenTests().isEmpty(),
                "At least one hidden test is required");
        require(notBlank(runtime.starterCode()) && notBlank(privateGrading.referenceSolution()),
                "Starter and reference code are required");
        require(runtime.publicTests().stream().allMatch(test -> "PUBLIC".equals(test.visibility())),
                "Public test visibility mismatch");
        require(privateGrading.hiddenTests().stream().allMatch(test -> "HIDDEN".equals(test.visibility())),
                "Hidden test visibility mismatch");
        require(runtime.publicTests().stream().allMatch(test -> test.points() == 0),
                "Public examples cannot award points");
        require(privateGrading.hiddenTests().stream().allMatch(test -> test.points() > 0),
                "Hidden tests must award points");
        require(ref(runtime.executionProfile(), "dsa_cpp17", "1.0.0"),
                "Unknown execution profile version");
        require(ref(runtime.comparator(), "exact_typed", "1.0.0"),
                "Unknown comparator version");
        require(runtime.archetype() != null && ARCHETYPES.contains(runtime.archetype().id()),
                "Unknown archetype");
        require(Set.of("1.0.0", "1.1.0").contains(runtime.archetype().version()),
                "Unknown archetype version");
        require(validFixture(runtime.signature().returnFixture()), "Unknown return fixture");

        List<String> argumentNames = runtime.signature().arguments().stream()
                .map(ArgumentSpec::name)
                .toList();
        require(new HashSet<>(argumentNames).size() == argumentNames.size(),
                "Function argument names must be unique");
        require(runtime.signature().arguments().stream()
                        .allMatch(argument -> notBlank(argument.name()) && validFixture(argument.fixture())),
                "Unknown argument fixture");

        List<TestCase> allTests = new ArrayList<>(runtime.publicTests());
        allTests.addAll(privateGrading.hiddenTests());
        require(allTests.stream().allMatch(test -> test != null
                        && notBlank(test.id())
                        && notBlank(test.category())
                        && test.arguments() != null
                        && new ArrayList<>(test.arguments().keySet()).equals(argumentNames)
                        && test.expectedOutput() != null
                        && !test.expectedOutput().isNull()),
                "Test arguments do not match the function signature");
        require(allTests.stream().map(TestCase::id).distinct().count() == allTests.size(),
                "Test ids must be unique");
        try {
            runtime.bannedApiPatterns().forEach(Pattern::compile);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid banned API pattern", exception);
        }
    }

    private static boolean validFixture(VersionedRef fixture) {
        return fixture != null && FIXTURES.contains(fixture.id()) && "1.0.0".equals(fixture.version());
    }

    private static boolean ref(VersionedRef value, String id, String version) {
        return value != null && id.equals(value.id()) && version.equals(value.version());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public record VersionedRef(String id, String version) {
    }

    public record ArgumentSpec(String name, VersionedRef fixture, Map<String, JsonNode> constraints) {
    }

    public record FunctionSignature(
            @JsonProperty("return_fixture") VersionedRef returnFixture,
            List<ArgumentSpec> arguments) {
    }

    public record TestCase(
            String id,
            String category,
            String visibility,
            Map<String, JsonNode> arguments,
            @JsonProperty("expected_output") JsonNode expectedOutput,
            double points) {
    }

    public record RuntimeContract(
            @JsonProperty("execution_profile") VersionedRef executionProfile,
            VersionedRef archetype,
            VersionedRef comparator,
            @JsonProperty("entry_point") String entryPoint,
            FunctionSignature signature,
            @JsonProperty("starter_code") String starterCode,
            @JsonProperty("public_tests") List<TestCase> publicTests,
            @JsonProperty("banned_api_patterns") List<String> bannedApiPatterns) {
    }

    public record PrivateGrading(
            @JsonProperty("reference_solution") String referenceSolution,
            @JsonProperty("hidden_tests") List<TestCase> hiddenTests) {
    }
}
