package edu.asu.ser516.metrics;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackageFanOutAggregationTest {

    private static final Path SAMPLE =
            Path.of("input/Simple-Java-Calculator/src");

    @Test
    void emptyInputProducesEmptyPackageFanOut() {
        CouplingAnalyzer analyzer = new CouplingAnalyzer(Path.of("input/empty"));
        analyzer.analyze();

        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut(); // DOES NOT EXIST YET

        assertTrue(pkgFanOut.isEmpty(),
                "Empty input should produce empty package-level fan-out");
    }

    @Test
    void sampleProjectProducesPackageFanOut() {
        CouplingAnalyzer analyzer = new CouplingAnalyzer(SAMPLE);
        analyzer.analyze();

        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        assertFalse(pkgFanOut.isEmpty(),
                "Sample project should produce package-level fan-out");
    }

    @Test
    void keysArePackageNamesNotClassNames() {
        CouplingAnalyzer analyzer = new CouplingAnalyzer(SAMPLE);
        analyzer.analyze();

        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        // Expect package name like "simplejavacalculator"
        assertTrue(pkgFanOut.keySet().stream()
                        .anyMatch(k -> k.contains("simplejavacalculator")),
                "Expected package-level key, not class-level key");
    }

    @Test
    void aggregationCollapsesMultipleClassesIntoSinglePackage() {
        CouplingAnalyzer analyzer = new CouplingAnalyzer(SAMPLE);
        analyzer.analyze();

        Map<String, Integer> classFanOut = analyzer.getFanOut();
        Map<String, Integer> pkgFanOut = analyzer.getPackageFanOut();

        assertTrue(pkgFanOut.size() < classFanOut.size(),
                "Package-level aggregation should reduce number of entries");
    }
}