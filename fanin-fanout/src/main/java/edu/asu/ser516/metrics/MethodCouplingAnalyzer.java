package edu.asu.ser516.metrics;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MethodCouplingAnalyzer {
    private final List<Path> sourceFiles;
    // Map from method fully qualified name to set of called method signatures
    private final Map<String, Set<String>> methodAdjacencyList = new HashMap<>();
    private final Set<String> projectMethods = new HashSet<>();

    public MethodCouplingAnalyzer(List<Path> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public void analyze() {
        Map<Path, CompilationUnit> parsedUnits = new HashMap<>();

        // Pass 1: Identify all methods defined in the project
        for (Path path : sourceFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(path);
                parsedUnits.put(path, cu);

                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");

                cu.findAll(MethodDeclaration.class).forEach(md -> {
                    Optional<ClassOrInterfaceDeclaration> parentClass = md
                            .findAncestor(ClassOrInterfaceDeclaration.class);
                    if (parentClass.isPresent()) {
                        String className = parentClass.get().getNameAsString();
                        String fqnClass = packageName.isEmpty() ? className : packageName + "." + className;
                        String methodSignature = fqnClass + "." + md.getSignature().asString();
                        projectMethods.add(methodSignature);
                    }
                });
            } catch (IOException e) {
                System.err.println("Failed to parse: " + path);
            }
        }

        // Pass 2: Calculate Fan-Out for each method
        for (Map.Entry<Path, CompilationUnit> entry : parsedUnits.entrySet()) {
            CompilationUnit cu = entry.getValue();
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            cu.findAll(MethodDeclaration.class).forEach(md -> {
                Optional<ClassOrInterfaceDeclaration> parentClass = md.findAncestor(ClassOrInterfaceDeclaration.class);
                if (parentClass.isPresent()) {
                    String className = parentClass.get().getNameAsString();
                    String fqnClass = packageName.isEmpty() ? className : packageName + "." + className;
                    String callerSignature = fqnClass + "." + md.getSignature().asString();

                    Set<String> dependencies = new HashSet<>();

                    // Without full symbol solving, we extract the method name called and use a
                    // combination of name and arguments as a footprint
                    md.findAll(MethodCallExpr.class).forEach(call -> {
                        String calledMethodName = call.getNameAsString();
                        int argsCount = call.getArguments().size();
                        String heuristicSignature = calledMethodName + "(" + argsCount + " args)";
                        dependencies.add(heuristicSignature);
                    });

                    methodAdjacencyList.put(callerSignature, dependencies);
                }
            });
        }
    }

    public Map<String, Integer> getFanOut() {
        return methodAdjacencyList.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    public Map<String, Integer> getFanIn() {
        Map<String, Integer> fanIn = new HashMap<>();
        projectMethods.forEach(m -> fanIn.put(m, 0));
        methodAdjacencyList.values().forEach(callees ->
                callees.forEach(callee -> {
                    if (fanIn.containsKey(callee)) {
                        fanIn.put(callee, fanIn.get(callee) + 1);
                    }
                })
        );

        return fanIn;
    }
}
