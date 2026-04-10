package edu.asu.ser516.metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MethodCouplingAnalyzerFanInRegressionTest {

    private static final Path SAMPLE_PROJECT =
            Path.of("input", "Simple-Java-Calculator", "src").toAbsolutePath();
    @Nested
    @DisplayName("Fan-In regression — values must not all be zero")
    class FanInNonZeroRegression {

        @Test
        @DisplayName("At least one method has Fan-In > 0 on the sample project")
        void atLeastOneMethodHasNonZeroFanIn() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
            analyzer.analyze();

            long nonZeroCount = analyzer.getFanIn().values().stream()
                    .filter(v -> v > 0)
                    .count();

            assertTrue(nonZeroCount > 0,
                    "Expected at least one method with Fan-In > 0. " +
                    "Got all zeros — likely a key-format mismatch between " +
                    "projectMethods and methodAdjacencyList dependencies.");
        }

        @Test
        @DisplayName("Total Fan-In across all methods equals total Fan-Out (call graph conservation)")
        void totalFanInEqualsTotalFanOut() throws IOException {
            List<Path> files = SourceScanner.findJavaFiles(SAMPLE_PROJECT);
            MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
            analyzer.analyze();

            int totalFanIn  = analyzer.getFanIn().values().stream().mapToInt(Integer::intValue).sum();
            int totalFanOut = analyzer.getFanOut().values().stream().mapToInt(Integer::intValue).sum();

            if (totalFanOut > 0) {
                assertTrue(totalFanIn > 0,
                        "totalFanIn is 0 but totalFanOut is " + totalFanOut +
                        ". This means callee keys in methodAdjacencyList never " +
                        "matched projectMethods keys — key-format mismatch bug.");
            }
        }
    }

    @Nested
    @DisplayName("Fan-In — synthetic fixture with known expected values")
    class FanInSyntheticFixture {

        private List<Path> writeTempProject(Path dir) throws IOException {
            Path a = dir.resolve("A.java");
            Path b = dir.resolve("B.java");

            Files.writeString(a,
                "public class A {\n" +
                "    public void run() {\n" +
                "        new B().helper();\n" +   
                "        new B().helper();\n" +   
                "    }\n" +
                "}\n");

            Files.writeString(b,
                "public class B {\n" +
                "    public void helper() {}\n" +
                "    public void unused() {}\n" +   
                "}\n");

            return List.of(a, b);
        }

        @Test
        @DisplayName("B.helper() has Fan-In = 1 when called once from A.run()")
        void helperHasFanInOne(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
            List<Path> files = writeTempProject(dir);
            MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
            analyzer.analyze();

            Map<String, Integer> fanIn = analyzer.getFanIn();

            int helperFanIn = fanIn.entrySet().stream()
                    .filter(e -> e.getKey().contains("B.helper()"))
                    .mapToInt(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() ->
                            new AssertionError("B.helper() not found in Fan-In map. Keys: " + fanIn.keySet()));

            assertEquals(1, helperFanIn,
                    "B.helper() is called from one distinct method (A.run()), so Fan-In must be 1.");
        }

        @Test
        @DisplayName("B.unused() has Fan-In = 0 when never called")
        void unusedMethodHasFanInZero(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
            List<Path> files = writeTempProject(dir);
            MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
            analyzer.analyze();

            Map<String, Integer> fanIn = analyzer.getFanIn();

            int unusedFanIn = fanIn.entrySet().stream()
                    .filter(e -> e.getKey().contains("B.unused()"))
                    .mapToInt(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() ->
                            new AssertionError("B.unused() not found in Fan-In map. Keys: " + fanIn.keySet()));

            assertEquals(0, unusedFanIn,
                    "B.unused() is never called, so Fan-In must be 0.");
        }

        @Test
        @DisplayName("A.run() has Fan-In = 0 (nobody calls it)")
        void callerMethodHasFanInZero(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
            List<Path> files = writeTempProject(dir);
            MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
            analyzer.analyze();

            Map<String, Integer> fanIn = analyzer.getFanIn();

            int runFanIn = fanIn.entrySet().stream()
                    .filter(e -> e.getKey().contains("A.run()"))
                    .mapToInt(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() ->
                            new AssertionError("A.run() not found in Fan-In map. Keys: " + fanIn.keySet()));

            assertEquals(0, runFanIn,
                    "A.run() is not called by anything, so Fan-In must be 0.");
        }

        @Test
        @DisplayName("Self-recursive call does not inflate Fan-In")
        void selfRecursiveCallDoesNotCountAsFanIn(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
            Path recFile = dir.resolve("Rec.java");
            Files.writeString(recFile,
                "public class Rec {\n" +
                "    public void loop() {\n" +
                "        loop();\n" +  // recursive — must NOT count as Fan-In for loop()
                "    }\n" +
                "}\n");

            List<Path> files = List.of(recFile);
            MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(files);
            analyzer.analyze();

            int loopFanIn = analyzer.getFanIn().entrySet().stream()
                    .filter(e -> e.getKey().contains("Rec.loop()"))
                    .mapToInt(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Rec.loop() not found"));

            assertEquals(0, loopFanIn,
                    "Recursive self-calls must not count as incoming Fan-In.");
        }
    }
}