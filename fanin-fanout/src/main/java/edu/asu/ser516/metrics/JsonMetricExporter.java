package edu.asu.ser516.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JsonMetricExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    public Path export(List<MetricRow> rows, Path outputDir) throws IOException {

        Path outputFile = ExportFileManager.prepareOutputFile(
                outputDir,
                rows.isEmpty() ? "unknown" : rows.get(0).getMetricType().name(),
                "json");

        ObjectNode root = mapper.createObjectNode();

        if (!rows.isEmpty()) {
            root.put("metric", rows.get(0).getMetricType().name());
            root.put("scope", rows.get(0).getScope().name());
        } else {
            root.put("metric", "UNKNOWN");
            root.put("scope", "UNKNOWN");
        }

        List<MetricRow> sortedRows = rows.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        ArrayNode resultsArray = mapper.createArrayNode();

        for (MetricRow row : sortedRows) {
            ObjectNode resultNode = mapper.createObjectNode();
            resultNode.put("entity", row.getEntity());
            resultNode.put("package", row.getPackageName());
            resultNode.put("file", row.getFilePath());
            resultNode.put("value", row.getValue());

            resultsArray.add(resultNode);
        }

        root.set("results", resultsArray);

        byte[] jsonBytes = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(root);
        ExportFileManager.atomicWrite(outputFile, jsonBytes);

        return outputFile;
    }
}