package org.service;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StorageConfig {

    private static final String DEFAULT_STORAGE_ROOT = "storage";

    public static Path getStorageRoot() {
        String env = System.getenv("STORAGE_ROOT");
        if (env != null && !env.isBlank()) {
            return Paths.get(env);
        }
        return Paths.get(DEFAULT_STORAGE_ROOT);
    }

    public static Path getClonePath() {
        return getStorageRoot().resolve("clones").resolve("clonedRepo");
    }

    public static Path getTempPath() {
        return getStorageRoot().resolve("temp").resolve("tempRepo");
    }

    public static Path getProcessedPath() {
        return getStorageRoot().resolve("processed");
    }
}