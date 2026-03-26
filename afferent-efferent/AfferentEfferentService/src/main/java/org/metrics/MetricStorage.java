package org.metrics;
import org.service.StorageConfig;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MetricStorage {

    public void storeAsCsv(List<MetricRecord> records) throws Exception {
        Path processedPath = StorageConfig.getProcessedPath();
        File directory = processedPath.toFile();

        if (!Files.exists(processedPath)) {
            directory.mkdirs();
        }

        File outputFile = processedPath.resolve("metrics_snapshot.csv").toFile();

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("date,metricName,metricValue\n");

            for (MetricRecord record : records) {
                writer.write(record.getDate() + "," +
                        record.getMetricName() + "," +
                        record.getMetricValue() + "\n");
            }
        }
    }
}