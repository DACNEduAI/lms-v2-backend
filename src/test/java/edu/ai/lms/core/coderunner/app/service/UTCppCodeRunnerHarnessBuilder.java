package edu.ai.lms.core.coderunner.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UTCppCodeRunnerHarnessBuilder {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CppCodeRunnerHarnessBuilder builder = new CppCodeRunnerHarnessBuilder(mapper);

    @Test
    void keepsExpectedOutputOutsideTheUntrustedProgram() {
        var test = test("hidden_secret", "HIDDEN", List.of(3, 1, 3), List.of(1, 3, 3), 1);
        String source = builder.buildCase(contract(test),
                "std::vector<int> quickSort(std::vector<int> values) { return values; }", test);

        assertTrue(source.contains("std::vector<int>{3, 1, 3}"));
        assertFalse(source.contains("std::vector<int>{1, 3, 3}"));
        assertFalse(source.contains("PASS"));
        assertFalse(source.contains("hidden_secret"));
        assertTrue(source.contains("static_assert"));
        assertTrue(source.contains("std::vector<int> (*)(std::vector<int>)"));
        assertEquals("V:3:1,3,3", builder.expectedOutput(contract(test), test));
    }

    @Test
    void rejectsLearnerMainFunction() {
        var test = test("public_001", "PUBLIC", List.of(), List.of(), 0);
        assertThrows(IllegalArgumentException.class,
                () -> builder.buildCase(contract(test), "int main() { return 0; }", test));
    }

    @Test
    void usesLengthPrefixedUtf8StringOutput() {
        var string = new ExecutableAssessmentContract.VersionedRef("string", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(string,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("value", string, Map.of())));
        Map<String, JsonNode> arguments = new LinkedHashMap<>();
        arguments.put("value", mapper.valueToTree("đề"));
        var publicTest = new ExecutableAssessmentContract.TestCase(
                "public_001", "unicode", "PUBLIC", arguments, mapper.valueToTree("đề"), 0);
        var hiddenTest = new ExecutableAssessmentContract.TestCase(
                "hidden_001", "unicode", "HIDDEN", arguments, mapper.valueToTree("đề"), 1);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "echo", signature, "std::string echo(std::string value) { return value; }",
                List.of(publicTest), List.of());
        var contract = new ExecutableAssessmentContract(
                "2.0.0", "echo_v1", "1.0.0", "READY_FOR_REVIEW", "Echo", "Echo",
                runtime, new ExecutableAssessmentContract.PrivateGrading(
                "std::string echo(std::string value) { return value; }", List.of(hiddenTest)));

        assertEquals("S:5:đề", builder.expectedOutput(contract, hiddenTest));
    }

    private ExecutableAssessmentContract contract(ExecutableAssessmentContract.TestCase selected) {
        var vector = new ExecutableAssessmentContract.VersionedRef("vector_int", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(vector,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("values", vector, Map.of())));
        var publicTest = selected.visibility().equals("PUBLIC")
                ? selected : test("public_001", "PUBLIC", List.of(2, 1), List.of(1, 2), 0);
        var hiddenTest = selected.visibility().equals("HIDDEN")
                ? selected : test("hidden_001", "HIDDEN", List.of(2, 1), List.of(1, 2), 1);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("sorting_function", "1.1.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "quickSort", signature,
                "std::vector<int> quickSort(std::vector<int> values) { return {}; }",
                List.of(publicTest), List.of());
        return new ExecutableAssessmentContract(
                "2.0.0", "quick_sort_v1", "1.0.0", "READY_FOR_REVIEW",
                "Quick sort", "Sort values", runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        "std::vector<int> quickSort(std::vector<int> values) { return values; }",
                        List.of(hiddenTest)));
    }

    private ExecutableAssessmentContract.TestCase test(
            String id, String visibility, List<Integer> input, List<Integer> expected, double points) {
        Map<String, JsonNode> arguments = new LinkedHashMap<>();
        arguments.put("values", mapper.valueToTree(input));
        return new ExecutableAssessmentContract.TestCase(
                id, "case", visibility, arguments, mapper.valueToTree(expected), points);
    }
}
