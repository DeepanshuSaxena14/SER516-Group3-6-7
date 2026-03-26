package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class OutgoingReferenceExtractor {
    private OutgoingReferenceExtractor() {}
    private static final class FileContext {
        final String pkg;
        final Set<String> importsExact;     
        final Set<String> importsWildcard;  
        FileContext(String pkg, Set<String> importsExact, Set<String> importsWildcard) {
            this.pkg = pkg;
            this.importsExact = importsExact;
            this.importsWildcard = importsWildcard;
        }
    }
    private static final Set<String> IGNORE_SIMPLE = Set.of(
            "String", "Object",
            "Integer", "Long", "Double", "Float", "Boolean", "Character", "Short", "Byte",
            "List", "Map", "Set", "Optional",
            "Override", "SuppressWarnings"
    );
    public static Map<String, Set<String>> extractOutgoingRefs(List<Path> javaFiles) {
        Set<String> projectClasses = ClassIndexBuilder.buildProjectClassIndex(javaFiles);
        return extractOutgoingRefs(javaFiles, projectClasses);
    }
    public static Map<String, Set<String>> extractOutgoingRefs(List<Path> javaFiles, Set<String> projectClasses) {
        Map<String, Set<String>> outgoing = new HashMap<>();
        JavaParser parser = new JavaParser();
        for (Path file : javaFiles) {
            CompilationUnit cu;
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.getResult().isEmpty()) {
                    System.out.println("WARN: Could not parse " + file);
                    continue;
                }
                cu = result.getResult().get();
            } catch (IOException ex) {
                System.out.println("WARN: IO error reading " + file + " -> " + ex.getMessage());
                continue;
            }
            FileContext ctx = buildContext(cu);
            for (TypeDeclaration<?> td : cu.getTypes()) {
                if (!(td instanceof ClassOrInterfaceDeclaration)) continue;
                String className = td.getNameAsString();
                String currentFqcn = ctx.pkg.isEmpty() ? className : ctx.pkg + "." + className;
                Set<String> deps = outgoing.computeIfAbsent(currentFqcn, k -> new HashSet<>());
                td.findAll(ObjectCreationExpr.class).forEach(expr -> {
                    String simple = expr.getType().getNameAsString();
                    addResolvedType(simple, ctx, currentFqcn, deps, projectClasses);
                });
                td.findAll(ClassOrInterfaceType.class).forEach(t -> {
                    String simple = t.getNameAsString();
                    addResolvedType(simple, ctx, currentFqcn, deps, projectClasses);
                });
            }
        }
        return outgoing;
    }
    private static FileContext buildContext(CompilationUnit cu) {
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        Set<String> exact = new HashSet<>();
        Set<String> wild = new HashSet<>();
        for (ImportDeclaration id : cu.getImports()) {
            if (id.isAsterisk()) wild.add(id.getNameAsString()); 
            else exact.add(id.getNameAsString());
        }
        return new FileContext(pkg, exact, wild);
    }
    private static void addResolvedType(String simpleName,
                                       FileContext ctx,
                                       String currentFqcn,
                                       Set<String> deps,
                                       Set<String> projectClasses) {
        if (simpleName == null || simpleName.isBlank()) return;
        if (IGNORE_SIMPLE.contains(simpleName)) return;
        for (String imp : ctx.importsExact) {
            if (imp.endsWith("." + simpleName)) {
                addDependency(imp, currentFqcn, deps);
                return; 
            }
        }
        if (!ctx.pkg.isEmpty()) {
            String samePkg = ctx.pkg + "." + simpleName;
            if (projectClasses.contains(samePkg)) {
                addDependency(samePkg, currentFqcn, deps);
                return;
            }
        }
        for (String basePkg : ctx.importsWildcard) {
            String candidate = basePkg + "." + simpleName;

            if (candidate.startsWith("java.") || candidate.startsWith("javax.")) {
                if (classExists(candidate)) {
                    addDependency(candidate, currentFqcn, deps);
                    return;
                }
            } else {
                if (projectClasses.contains(candidate)) {
                    addDependency(candidate, currentFqcn, deps);
                    return;
                }
            }
        }
    }
    private static boolean classExists(String fqcn) {
        try {
            Class.forName(fqcn, false, OutgoingReferenceExtractor.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    private static void addDependency(String candidateFqcn, String currentFqcn, Set<String> deps) {
        if (candidateFqcn == null) return;
        if (candidateFqcn.equals(currentFqcn)) return;
        deps.add(candidateFqcn);
    }
}