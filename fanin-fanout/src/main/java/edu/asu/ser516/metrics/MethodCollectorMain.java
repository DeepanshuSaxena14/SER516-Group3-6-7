package edu.asu.ser516.metrics;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MethodCollectorMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: MethodCollectorMain <source-root>");
            System.exit(1);
        }

        Path root = Path.of(args[0]);
        List<Path> files = SourceScanner.findJavaFiles(root);

        MethodDeclarationCollector collector = new MethodDeclarationCollector();
        Map<String, MethodDeclaration> methods = collector.collectMethods(files);

        System.out.println("Total methods found: " + methods.size());
        methods.keySet().forEach(System.out::println);
    }
}