package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClassIndexBuilder {

    private ClassIndexBuilder() { }

    public static Set<String> buildProjectClassIndex(List<Path> javaFiles) {
        Set<String> projectClasses = new HashSet<>();
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        JavaParser parser = new JavaParser(config);

        for (Path file : javaFiles) {
            CompilationUnit cu;

            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.getResult().isEmpty()) {
                    System.out.println("WARN: Could not parse " + file);
                    result.getProblems().forEach(p -> System.out.println("  - " + p));
                    continue;
                }
                cu = result.getResult().get();
            } catch (IOException ex) {
                System.out.println("WARN: IO error reading " + file + " -> " + ex.getMessage());
                continue;
            }

            String pkg = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            for (TypeDeclaration<?> td : cu.getTypes()) {
                if (td instanceof ClassOrInterfaceDeclaration) {
                    String typeName = td.getNameAsString();
                    String fqcn = pkg.isEmpty() ? typeName : pkg + "." + typeName;
                    projectClasses.add(fqcn);
                }
            }
        }

        return projectClasses;
    }
}