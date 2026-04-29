package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MethodFanInAnalyzer {

    public Map<String, Integer> computeFanIn(Iterable<Path> javaFiles) {
        MethodDeclarationCollector collector = new MethodDeclarationCollector();
        Map<String, MethodDeclaration> declaredMethods;

        try {
            declaredMethods = collector.collectMethods(javaFiles);
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect declared methods", e);
        }

        Map<String, Integer> fanInCounts = new HashMap<>();
        for (String methodKey : declaredMethods.keySet()) {
            fanInCounts.put(methodKey, 0);
        }

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

                cu.findAll(MethodDeclaration.class).forEach(callerMethod -> {
                    String callerClass = callerMethod.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::getNameAsString)
                            .orElse("UnknownClass");

                    String callerKey = buildMethodKey(
                            packageName,
                            callerClass,
                            callerMethod.getNameAsString(),
                            callerMethod.getParameters().size());

                    Set<String> uniqueCalleesFromCaller = new HashSet<>();

                    callerMethod.findAll(MethodCallExpr.class).forEach(call -> {
                        String calledMethodName = call.getNameAsString();
                        int argCount = call.getArguments().size();

                        for (String declaredMethod : declaredMethods.keySet()) {
                            if (declaredMethod.endsWith("." + calledMethodName + "(" + argCount + ")")) {
                                uniqueCalleesFromCaller.add(declaredMethod);
                            }
                        }
                    });

                    for (String calleeKey : uniqueCalleesFromCaller) {
                        if (!calleeKey.equals(callerKey)) {
                            fanInCounts.put(calleeKey, fanInCounts.getOrDefault(calleeKey, 0) + 1);
                        }
                    }
                });

            } catch (Exception e) {
                System.err.println("Failed to process file: " + file + " -> " + e.getMessage());
            }
        }

        return fanInCounts;
    }

    private String buildMethodKey(String packageName, String className, String methodName, int paramCount) {
        if (packageName == null || packageName.isBlank()) {
            return className + "." + methodName + "(" + paramCount + ")";
        }
        return packageName + "." + className + "." + methodName + "(" + paramCount + ")";
    }
}