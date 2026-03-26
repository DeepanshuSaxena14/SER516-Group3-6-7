package edu.asu.ser516.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Path;

public class SingleFileParserMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: SingleFileParserMain <path-to-java-file>");
            System.exit(1);
        }
        Path file = Path.of(args[0]);
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(file);
        if (result.getResult().isEmpty()) {
            System.out.println("Parse failed: " + file);
            result.getProblems().forEach(p -> System.out.println(" - " + p));
            System.exit(2);
        }
        CompilationUnit cu = result.getResult().get();
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("(default)");
        System.out.println("File: " + file);
        System.out.println("Package: " + pkg);
        System.out.println("Imports:");
        cu.getImports().forEach(i -> System.out.println(" - " + i.getNameAsString() + (i.isAsterisk() ? ".*" : "")));
        System.out.println("Top-level types:");
        cu.getTypes().forEach(t -> System.out.println(" - " + t.getNameAsString()));
    }
}
