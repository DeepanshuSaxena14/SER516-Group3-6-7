package edu.asu.ser516.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class JsonMetricExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    public Path export(List<MetricRow> rows, Path outputDir) throws IOException {
        List<MetricRow> safeRows = (rows == null) ? Collections.emptyList() : rows;

        Path outputFile = ExportFileManager.prepareOutputFile(
                outputDir,
                "metrics",
                "json");

        ObjectNode root = mapper.createObjectNode();
        root.put("exportType", "METRICS");
        root.put("rowCount", safeRows.size());

        List<MetricRow> sortedRows = safeRows.stream()
                .sorted((a, b) -> {
                    int cmp = a.getMetricType().name().compareTo(b.getMetricType().name());
                    if (cmp != 0) return cmp;
                    cmp = a.getScope().name().compareTo(b.getScope().name());
                    if (cmp != 0) return cmp;
                    cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getEntity().compareTo(b.getEntity());
                })
                .toList();

        ArrayNode resultsArray = mapper.createArrayNode();

        for (MetricRow row : sortedRows) {
            ObjectNode resultNode = mapper.createObjectNode();
            resultNode.put("metricType", row.getMetricType().name());
            resultNode.put("scope", row.getScope().name());
            resultNode.put("entity", row.getEntity());
            resultNode.put("package", row.getPackageName());
            resultNode.put("file", row.getFilePath());
            resultNode.put("value", row.getValue());
            resultsArray.add(resultNode);
        }

        root.set("results", resultsArray);

        byte[] jsonBytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
        ExportFileManager.atomicWrite(outputFile, jsonBytes);

        return outputFile;
    }
}