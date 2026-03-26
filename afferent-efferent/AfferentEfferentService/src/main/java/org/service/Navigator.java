package org.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

import java.util.List;

public class Navigator {

    public static List<Path> navigateRepo(String repoPath) throws IOException {

        try (Stream<Path> paths = Files.walk(Paths.get(repoPath))) {
            return paths
                    .filter(path -> Files.isRegularFile(path))
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }
    }


}
