package org.service;

import java.nio.file.Path;
import java.util.*;

public class Analyzer {

    public static List<Metrics> analyze(String repoPath)
            throws Exception {

        List<Path> files = Navigator.navigateRepo(repoPath);
        List<FileData> parsedFiles = new ArrayList<>();

        for (Path file : files) {
            parsedFiles.add(Parser.parse(file));
        }
        Set<String> internalClasses = new HashSet<>();
        for (FileData file : parsedFiles) {
            internalClasses.add(file.className());
        }

        Map<String, Set<String>> afferentMap = new HashMap<>();
        for (FileData file : parsedFiles) {
            for (String imp : file.imports()) {
                if (internalClasses.contains(imp)) {
                    afferentMap
                            .computeIfAbsent(imp,
                                    k -> new HashSet<>())
                            .add(file.className());
                }
            }
        }

        //IT'S EMETRIC... something something party ride, the emetric slide.
        List<Metrics> results = new ArrayList<>();
                for (FileData file : parsedFiles) {
            int efferent = (int) file.imports()
                    .stream()
                    .filter(internalClasses::contains)
                    .count();
            int afferent = afferentMap
                    .getOrDefault(file.className(),
                            Collections.emptySet())
                    .size();
            results.add(
                    new Metrics(
                            file.className(),
                            afferent,
                            efferent
                    )
            );
        }
        return results;
    }
}