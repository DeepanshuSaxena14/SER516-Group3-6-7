package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CouplingAnalyzer {
    private final List<Path> sourceFiles;
    private final Set<String> projectClasses = new HashSet<>();
    private final Map<String, Set<String>> adjacencyList = new HashMap<>();

    public CouplingAnalyzer(List<Path> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public void analyze() {
        // Pass 1: Identify all types defined in the project (fully qualified)
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

                cu.findAll(TypeDeclaration.class).forEach(td -> {
                    if (td.isTopLevelType()) {
                        String fqn = packageName.isEmpty() ? td.getNameAsString()
                                : packageName + "." + td.getNameAsString();
                        projectClasses.add(fqn);
                    }
                });
            } catch (IOException e) {
                System.err.println("Failed to parse: " + path);
            }
        }

        // Pass 2: Calculate Fan-Out
        for (Map.Entry<Path, CompilationUnit> entry : parsedUnits.entrySet()) {
            CompilationUnit cu = entry.getValue();
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            List<ImportDeclaration> imports = cu.getImports();

            cu.findAll(TypeDeclaration.class).forEach(td -> {
                if (!td.isTopLevelType())
                    return;

                String sourceClass = packageName.isEmpty() ? td.getNameAsString()
                        : packageName + "." + td.getNameAsString();
                Set<String> dependencies = new HashSet<>();

                td.findAll(ClassOrInterfaceType.class).forEach(type -> {
                    String typeName = type.getNameAsString();
                    String resolved = resolve(typeName, packageName, imports);

                    if (resolved != null && !resolved.equals(sourceClass)) {
                        dependencies.add(resolved);
                    }
                });

                adjacencyList.put(sourceClass, dependencies);
            });
        }
    }

    private String resolve(String typeName, String currentPackage, List<ImportDeclaration> imports) {
        // 1. Check if it's already a fully qualified name in our project
        if (projectClasses.contains(typeName)) {
            return typeName;
        }

        // 2. Check same package
        String samePackageCandidate = currentPackage.isEmpty() ? typeName : currentPackage + "." + typeName;
        if (projectClasses.contains(samePackageCandidate)) {
            return samePackageCandidate;
        }

        // 3. Check explicit imports
        for (ImportDeclaration imp : imports) {
            String importedName = imp.getNameAsString();
            if (!imp.isAsterisk()) {
                if (importedName.endsWith("." + typeName)) {
                    if (projectClasses.contains(importedName)) {
                        return importedName;
                    }
                }
            }
        }

        // 4. Check wildcard imports (expensive, but necessary)
        for (ImportDeclaration imp : imports) {
            if (imp.isAsterisk()) {
                String packagePrefix = imp.getNameAsString(); // e.g. "com.example"
                String candidate = packagePrefix + "." + typeName;
                if (projectClasses.contains(candidate)) {
                    return candidate;
                }
            }
        }

        return null; // Not an internal project class
    }

    public Map<String, Integer> getFanOut() {
        return adjacencyList.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    public Map<String, Integer> getFanIn() {
        Map<String, Integer> fanIn = new HashMap<>();
        projectClasses.forEach(c -> fanIn.put(c, 0));

        adjacencyList.values().forEach(deps -> {
            deps.forEach(dep -> {
                fanIn.put(dep, fanIn.getOrDefault(dep, 0) + 1);
            });
        });
        return fanIn;
    }

    public Map<String, Integer> getPackageFanOut() {
        Map<String, Integer> classFanOut = getFanOut();
        Map<String, Integer> packageFanOut = new HashMap<>();

        for (Map.Entry<String, Integer> entry : classFanOut.entrySet()) {
            String className = entry.getKey();
            int fanOut = entry.getValue();

            String packageName = className.contains(".") ? className.substring(0, className.lastIndexOf('.'))
                    : "(default)";
            packageFanOut.merge(packageName, fanOut, Integer::sum);
        }

        return packageFanOut;
    }

    public Map<String, Integer> getPackageFanIn() {
        Map<String, Integer> classFanIn = getFanIn();
        Map<String, Integer> packageFanIn = new HashMap<>();

        for (Map.Entry<String, Integer> entry : classFanIn.entrySet()) {
            String className = entry.getKey();
            int fanIn = entry.getValue();

            String packageName = className.contains(".")
                    ? className.substring(0, className.lastIndexOf('.'))
                    : "(default)";

            packageFanIn.merge(packageName, fanIn, Integer::sum);
        }

        return packageFanIn;
    }

    public Map<String, Integer> getProjectFanIn() {
        int total = getFanIn().values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        Map<String, Integer> projectFanIn = new LinkedHashMap<>();
        projectFanIn.put("project", total);
        return projectFanIn;
    }
}
