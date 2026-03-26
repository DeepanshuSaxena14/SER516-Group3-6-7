package edu.asu.ser516.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MethodCouplingAnalyzerTest {

    private static final Path SAMPLE_PROJECT =
            Paths.get("input", "Simple-Java-Calculator", "src").toAbsolutePath();

    private MethodCouplingAnalyzer analyzeProject(List<Path> files) {
        MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
        analyzer.analyze();
        return analyzer;
    }

    @Nested
    @DisplayName("Fan-Out — Empty and boundary inputs")
    class FanOutEmptyInputs {

        @Test
        @DisplayName("Empty file list returns empty Fan-Out map")
        void emptyFileListReturnsEmptyFanOut() {
            MethodCouplingAnalyzer analyzer = analyzeProject(List.of());
            assertTrue(analyzer.getFanOut().isEmpty(),
                    "Expected empty Fan-Out map for empty file list");
        }
    }

    @Nested
    @DisplayName("Fan-Out — Correct values")
    class FanOutCorrectValues {

        @Test
        @DisplayName("Fan-Out map is non-empty for the sample project")
        void fanOutMapNonEmptyForSampleProject() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            assertFalse(analyzer.getFanOut().isEmpty(),
                    "Sample project must produce a non-empty Fan-Out map");
        }

        @Test
        @DisplayName("All Fan-Out values are non-negative")
        void allFanOutValuesNonNegative() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            analyzer.getFanOut().forEach((method, fanOut) ->
                    assertTrue(fanOut >= 0,
                            method + " has negative fanOut: " + fanOut));
        }

        @Test
        @DisplayName("Method keys use FQCN format: package.ClassName.methodSignature")
        void methodKeysUseFqcnFormat() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            analyzer.getFanOut().keySet().forEach(key ->
                    assertTrue(key.contains("."),
                            "Method key must contain package separator: " + key));
        }

        @Test
        @DisplayName("UI.actionPerformed has fanOut >= 1 (calls multiple Calculator methods)")
        void uiActionPerformedHasNonZeroFanOut() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            boolean found = analyzer.getFanOut().entrySet().stream()
                    .anyMatch(e -> e.getKey().contains("actionPerformed")
                            && e.getValue() >= 1);
            assertTrue(found,
                    "actionPerformed must have fanOut >= 1 — it calls Calculator methods");
        }

        @Test
        @DisplayName("Calling getFanOut() twice returns the same result (idempotent)")
        void getFanOutIsIdempotent() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            assertEquals(analyzer.getFanOut(), analyzer.getFanOut(),
                    "getFanOut() must return identical results on repeated calls");
        }
    }

    @Nested
    @DisplayName("Fan-In — Empty and boundary inputs")
    class FanInEmptyInputs {

        @Test
        @DisplayName("Empty file list returns empty Fan-In map")
        void emptyFileListReturnsEmptyFanIn() {
            MethodCouplingAnalyzer analyzer = analyzeProject(List.of());
            assertTrue(analyzer.getFanIn().isEmpty(),
                    "Expected empty Fan-In map for empty file list");
        }
    }

    @Nested
    @DisplayName("Fan-In — Correct values")
    class FanInCorrectValues {

        @Test
        @DisplayName("Fan-In map is non-empty for the sample project")
        void fanInMapNonEmptyForSampleProject() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            assertFalse(analyzer.getFanIn().isEmpty(),
                    "Sample project must produce a non-empty Fan-In map");
        }

        @Test
        @DisplayName("All Fan-In values are non-negative")
        void allFanInValuesNonNegative() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            analyzer.getFanIn().forEach((method, fanIn) ->
                    assertTrue(fanIn >= 0,
                            method + " has negative fanIn: " + fanIn));
        }

        @Test
        @DisplayName("Fan-In map contains all project methods seeded with 0")
        void fanInMapContainsAllProjectMethods() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            Map<String, Integer> fanOut = analyzer.getFanOut();
            Map<String, Integer> fanIn  = analyzer.getFanIn();

            fanOut.keySet().forEach(method ->
                    assertTrue(fanIn.containsKey(method),
                            "Method must be seeded in Fan-In map: " + method));

            fanIn.values().forEach(v ->
                    assertTrue(v >= 0,
                            "All Fan-In values must be non-negative"));
        }

        @Test
        @DisplayName("Calling getFanIn() twice returns the same result (idempotent)")
        void getFanInIsIdempotent() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            assertEquals(analyzer.getFanIn(), analyzer.getFanIn(),
                    "getFanIn() must return identical results on repeated calls");
        }
    }

    @Nested
    @DisplayName("Fan-In and Fan-Out consistency")
    class Consistency {

        @Test
        @DisplayName("Methods in Fan-Out map also appear in the Fan-In map")
        void fanOutMethodsAppearInFanIn() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            Map<String, Integer> fanOut = analyzer.getFanOut();
            Map<String, Integer> fanIn  = analyzer.getFanIn();

            fanOut.keySet().forEach(method ->
                    assertTrue(fanIn.containsKey(method),
                            "Method in Fan-Out must also appear in Fan-In map: " + method));
        }

        @Test
        @DisplayName("All Fan-Out keys also appear in the Fan-In map")
        void fanOutKeysAreSubsetOfFanInKeys() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = analyzeProject(files);

            Map<String, Integer> fanOut = analyzer.getFanOut();
            Map<String, Integer> fanIn  = analyzer.getFanIn();

            fanOut.keySet().forEach(method ->
                    assertTrue(fanIn.containsKey(method),
                            "Method in Fan-Out must also appear in Fan-In map: " + method));
        }
    }
}