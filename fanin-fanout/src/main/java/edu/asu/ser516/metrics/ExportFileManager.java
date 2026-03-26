package edu.asu.ser516.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExportFileManager {

    public static Path prepareOutputFile(Path outputDir, String metricName, String extension) throws IOException {

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        String fileName = metricName.toLowerCase()
                + "_" + System.currentTimeMillis()
                + "." + extension;

        return outputDir.resolve(fileName);
    }

    public static void atomicWrite(Path finalPath, byte[] content) throws IOException {
        Path parent = finalPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Path tmpPath = finalPath.resolveSibling(finalPath.getFileName() + ".tmp");
        try {
            Files.write(tmpPath, content);
            Files.move(tmpPath, finalPath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tmpPath);
            throw e;
        }
    }
}