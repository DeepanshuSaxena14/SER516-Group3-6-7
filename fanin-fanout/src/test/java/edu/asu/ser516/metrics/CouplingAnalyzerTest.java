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

public class CouplingAnalyzerTest {
    private static final Path SAMPLE_PROJECT =
            Paths.get("input", "Simple-Java-Calculator", "src").toAbsolutePath();

    private Map<String, Integer> fanInFor(List<Path> files) {
        CouplingAnalyzer analyzer = new CouplingAnalyzer(files);
        analyzer.analyze();
        return analyzer.getFanIn();
    }

    @Nested
    @DisplayName("Empty and boundary inputs")
    class EmptyAndBoundaryInputs {
        @Test
        @DisplayName("Empty file list returns empty map")
        void emptyFileListReturnsEmptyMap() {
            Map<String, Integer> result = fanInFor(List.of());
            assertTrue(result.isEmpty(), "Expected empty map for empty input");
        }

        @Nested
        @DisplayName("All project classes appear in the result")
        class AllClassesPresent {
            @Test
            @DisplayName("Every class in the sample project appears as a key in the Fan-In result")
            void allSampleClassesAppearInResult() throws IOException {
                List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
                Map<String, Integer> result = fanInFor(files);

                assertTrue(result.containsKey("simplejavacalculator.UI"),
                        "UI must appear in Fan-In result");
                assertTrue(result.containsKey("simplejavacalculator.Calculator"),
                        "Calculator must appear in Fan-In result");
                assertTrue(result.containsKey("simplejavacalculator.BufferedImageCustom"),
                        "BufferedImageCustom must appear in Fan-In result");
                assertTrue(result.containsKey("simplejavacalculator.SimpleJavaCalculator"),
                        "SimpleJavaCalculator must appear in Fan-In result");
            }

            @Test
            @DisplayName("A class with no inbound references still appears with fanIn = 0")
            void unreferencedClassAppearsWithZero() throws IOException {
                List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
                Map<String, Integer> result = fanInFor(files);

                assertTrue(result.containsKey("simplejavacalculator.SimpleJavaCalculator"),
                        "Entry-point class must still appear in the result");
                assertEquals(0, result.get("simplejavacalculator.SimpleJavaCalculator"),
                        "Entry-point class should have fanIn = 0");
            }
        }

        @Nested
        @DisplayName("Correct Fan-In values")
        class CorrectFanInValues {
            @Test
            @DisplayName("All Fan-In values are non-negative")
            void allFanInValuesNonNegative() throws IOException {
                List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
                Map<String, Integer> result = fanInFor(files);

                result.forEach((cls, fanIn) ->
                        assertTrue(fanIn >= 0,
                                cls + " has negative fanIn value: " + fanIn));
            }
        }

        @Nested
        @DisplayName("Idempotency")
        class Idempotency {

            @Test
            @DisplayName("Calling getFanIn() twice on the same analyzer returns the same result")
            void getFanInIsIdempotent() throws IOException {
                List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
                CouplingAnalyzer analyzer = new CouplingAnalyzer(files);
                analyzer.analyze();

                Map<String, Integer> result1 = analyzer.getFanIn();
                Map<String, Integer> result2 = analyzer.getFanIn();

                assertEquals(result1, result2,
                        "getFanIn() must return the same result on repeated calls");
            }
        }

        @Nested
        @DisplayName("Fan-In and Fan-Out consistency")
        class FanInFanOutConsistency {
            @Test
            @DisplayName("Fan-In and Fan-Out maps are both non-empty for the sample project")
            void bothMapsNonEmpty() throws IOException {
                List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
                CouplingAnalyzer analyzer = new CouplingAnalyzer(files);
                analyzer.analyze();

                Map<String, Integer> fanIn  = analyzer.getFanIn();
                Map<String, Integer> fanOut = analyzer.getFanOut();

                assertFalse(fanOut.isEmpty(), "Fan-Out map must not be empty");
                assertFalse(fanIn.isEmpty(),  "Fan-In map must not be empty");
            }
        }
    }
}
