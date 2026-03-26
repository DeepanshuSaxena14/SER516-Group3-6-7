package edu.asu.ser516.metrics;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OutgoingRefMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: OutgoingRefMain <path-to-java-project>");
            System.exit(1);
        }
        Path root = Path.of(args[0]);
        List<java.nio.file.Path> javaFiles = SourceScanner.findJavaFiles(root);
        Map<String, Set<String>> outgoing =
                OutgoingReferenceExtractor.extractOutgoingRefs(javaFiles);
        outgoing.forEach((cls, deps) -> {
            System.out.println(cls + " -> " + deps.size() + " deps");
            deps.stream().sorted().forEach(d -> System.out.println("   - " + d));
        });
    }
}