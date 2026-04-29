package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseResult;
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
    private final Map<String, Set<String>> methodAdjacencyList = new HashMap<>();
    private final Set<String> projectMethods = new HashSet<>();
    private final Map<String, Set<String>> callLookup = new HashMap<>();

    public MethodCouplingAnalyzer(List<Path> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    @SuppressWarnings("unchecked")
    public void analyze() {
        Map<Path, CompilationUnit> parsedUnits = new HashMap<>();
        for (Path path : sourceFiles) {
            try {
                ParserConfiguration config = new ParserConfiguration()
                        .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
                JavaParser parser = new JavaParser(config);
                ParseResult<CompilationUnit> result = parser.parse(path);
                if (result.getResult().isEmpty()) {
                    System.err.println("Parse failed: " + path);
                    continue;
                }
                CompilationUnit cu = result.getResult().get();
                parsedUnits.put(path, cu);

                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");

                cu.findAll(MethodDeclaration.class).forEach(md -> {
                    Optional<ClassOrInterfaceDeclaration> parentClass =
                            md.findAncestor(ClassOrInterfaceDeclaration.class);
                    if (parentClass.isPresent()) {
                        String className = parentClass.get().getNameAsString();
                        String fqnClass = packageName.isEmpty()
                                ? className
                                : packageName + "." + className;
                        String fqnSignature = fqnClass + "." + md.getSignature().asString();
                        projectMethods.add(fqnSignature);
                        String lookupKey = md.getNameAsString()
                                + "(" + md.getParameters().size() + ")";
                        callLookup
                                .computeIfAbsent(lookupKey, k -> new HashSet<>())
                                .add(fqnSignature);
                    }
                });
            } catch (IOException e) {
                System.err.println("Failed to parse: " + path);
            }
        }

        for (Map.Entry<Path, CompilationUnit> entry : parsedUnits.entrySet()) {
            CompilationUnit cu = entry.getValue();
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            cu.findAll(MethodDeclaration.class).forEach(md -> {
                Optional<ClassOrInterfaceDeclaration> parentClass =
                        md.findAncestor(ClassOrInterfaceDeclaration.class);
                if (parentClass.isPresent()) {
                    String className = parentClass.get().getNameAsString();
                    String fqnClass = packageName.isEmpty()
                            ? className
                            : packageName + "." + className;
                    String callerSignature = fqnClass + "." + md.getSignature().asString();

                    Set<String> dependencies = new HashSet<>();

                    md.findAll(MethodCallExpr.class).forEach(call -> {
                        String calledName = call.getNameAsString();
                        int argCount     = call.getArguments().size();
                        String lookupKey = calledName + "(" + argCount + ")";
                        Set<String> matches = callLookup.getOrDefault(lookupKey, Collections.emptySet());

                        for (String match : matches) {
                            if (!match.equals(callerSignature)) {
                                dependencies.add(match);
                            }
                        }
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
                        fanIn.merge(callee, 1, Integer::sum);
                    }
                })
        );

        return fanIn;
    }
}