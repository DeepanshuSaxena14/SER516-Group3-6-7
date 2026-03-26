package edu.asu.ser516.metrics;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ClassIndexMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: ClassIndexMain <path-to-java-project>");
            System.exit(1);
        }

        Path root = Path.of(args[0]);

        List<java.nio.file.Path> javaFiles = SourceScanner.findJavaFiles(root);
        System.out.println("Found .java files: " + javaFiles.size());

        Set<String> classes = ClassIndexBuilder.buildProjectClassIndex(javaFiles);

        System.out.println("Project classes indexed: " + classes.size());
        classes.stream().sorted().limit(20).forEach(c -> System.out.println(" - " + c));
    }
}