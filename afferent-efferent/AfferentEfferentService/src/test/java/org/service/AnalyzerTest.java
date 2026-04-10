package org.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyzerTest {

    @Test
    void computesAfferentAndEfferentForSimpleProject() throws Exception {
        Path repo = Files.createTempDirectory("repo");

        Path pkgDir = repo.resolve("com/example");
        Files.createDirectories(pkgDir);

        Files.writeString(pkgDir.resolve("A.java"), """
                package com.example;
                import com.example.B;
                public class A { }
                """);

        Files.writeString(pkgDir.resolve("B.java"), """
                package com.example;
                public class B { }
                """);

        List<Metrics> results = Analyzer.analyze(repo.toString());

        Metrics a = results.stream()
                .filter(m -> m.getClassName().equals("com.example.A"))
                .findFirst()
                .orElseThrow();

        Metrics b = results.stream()
                .filter(m -> m.getClassName().equals("com.example.B"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, a.getAfferent());
        assertEquals(1, a.getEfferent());

        assertEquals(1, b.getAfferent());
        assertEquals(0, b.getEfferent());
    }
}