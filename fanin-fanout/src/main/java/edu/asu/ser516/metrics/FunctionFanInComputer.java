package edu.asu.ser516.metrics;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Computes method-level fan-in: for each project method, counts how many other methods invoke it.
 */
public final class FunctionFanInComputer {

    private FunctionFanInComputer() {}

    public static Map<String, Integer> compute(List<Path> javaFiles) {
        MethodCouplingAnalyzer analyzer = new MethodCouplingAnalyzer(javaFiles);
        analyzer.analyze();
        return analyzer.getFanIn();
    }
}
