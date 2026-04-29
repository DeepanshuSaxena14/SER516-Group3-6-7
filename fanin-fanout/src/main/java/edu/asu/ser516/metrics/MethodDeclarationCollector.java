package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class MethodDeclarationCollector {

    public Map<String, MethodDeclaration> collectMethods(Iterable<Path> javaFiles) throws IOException {
        Map<String, MethodDeclaration> methods = new TreeMap<>();
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        JavaParser parser = new JavaParser(config);

        for (Path file : javaFiles) {
            try {
                var result = parser.parse(file);
                if (result.getResult().isEmpty()) {
                    System.err.println("Parse failed: " + file);
                    continue;
                }

                CompilationUnit cu = result.getResult().get();
                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");

                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::getNameAsString)
                            .orElse("UnknownClass");

                    String methodKey = buildMethodKey(packageName, className, method);
                    methods.put(methodKey, method);
                });

            } catch (Exception e) {
                System.err.println("Failed to parse file: " + file + " -> " + e.getMessage());
            }
        }

        return methods;
    }

    private String buildMethodKey(String packageName, String className, MethodDeclaration method) {
        String methodName = method.getNameAsString();
        int paramCount = method.getParameters().size();

        if (packageName == null || packageName.isBlank()) {
            return className + "." + methodName + "(" + paramCount + ")";
        }

        return packageName + "." + className + "." + methodName + "(" + paramCount + ")";
    }
}