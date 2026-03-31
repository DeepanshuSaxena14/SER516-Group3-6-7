package edu.asu.ser516.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PackageFanOutAggregationTest {

    @TempDir
    Path tempDir;

    private List<Path> javaFilesUnder(Path root) throws IOException {
        return Files.walk(root)
                .filter(p -> p.toString().endsWith(".java"))
                .toList();
    }

    @Test
    void emptyInputProducesEmptyPackageFanOut() throws Exception {
        Path emptyRoot = Files.createDirectory(tempDir.resolve("empty"));

        CouplingAnalyzer analyzer = new CouplingAnalyzer(javaFilesUnder(emptyRoot));
        analyzer.analyze();

        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        assertTrue(pkgFanOut.isEmpty(),
                "Empty input should produce an empty package-level fan-out map");
    }

    @Test
    void sampleProjectProducesPackageFanOut() throws Exception {
        Path sampleRoot = Path.of("input/Simple-Java-Calculator/src");

        CouplingAnalyzer analyzer = new CouplingAnalyzer(javaFilesUnder(sampleRoot));
        analyzer.analyze();

        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        assertFalse(pkgFanOut.isEmpty(),
                "Sample project should produce package-level fan-out entries");
    }

    @Test
    void keysArePackageNamesNotClassNames() throws Exception {
        Path sampleRoot = Path.of("input/Simple-Java-Calculator/src");

        CouplingAnalyzer analyzer = new CouplingAnalyzer(javaFilesUnder(sampleRoot));
        analyzer.analyze();

        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        assertTrue(
                pkgFanOut.keySet().stream().anyMatch(k -> k.contains("simplejavacalculator")),
                "Package-level keys should use package names, not class names");
    }

    @Test
    void aggregationCollapsesMultipleClassEntries() throws Exception {
        Path sampleRoot = Path.of("input/Simple-Java-Calculator/src");

        CouplingAnalyzer analyzer = new CouplingAnalyzer(javaFilesUnder(sampleRoot));
        analyzer.analyze();

        Map<String, Integer> classFanOut = analyzer.getFanOut();
        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        assertTrue(
                pkgFanOut.size() < classFanOut.size(),
                "Package-level aggregation should reduce the number of entries");
    }

    public Map<String, Integer> getPackageFanOut() {
        return Collections.emptyMap();
    }
}