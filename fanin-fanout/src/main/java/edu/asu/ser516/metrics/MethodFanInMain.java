package edu.asu.ser516.metrics;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MethodFanInMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: MethodFanInMain <source-root>");
            System.exit(1);
        }

        Path root = Path.of(args[0]);
        List<Path> files = SourceScanner.findJavaFiles(root);

        MethodFanInAnalyzer analyzer = new MethodFanInAnalyzer();
        Map<String, Integer> fanIn = analyzer.computeFanIn(files);

        System.out.println("Total methods analyzed: " + fanIn.size());
        fanIn.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .forEach(entry -> System.out.println(entry.getKey() + " -> " + entry.getValue()));
    }
}