package org.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    void parsesPackageClassAndImports() throws Exception {
        Path tempFile = Files.createTempFile("ParserTest", ".java");
        Files.writeString(tempFile, """
                package com.example.demo;

                import java.util.List;
                import com.example.other.Helper;

                public class SampleClass {
                }
                """);

        FileData data = Parser.parse(tempFile);

        assertEquals("com.example.demo.SampleClass", data.className());
        assertTrue(data.imports().contains("java.util.List"));
        assertTrue(data.imports().contains("com.example.other.Helper"));
    }
}