package edu.asu.ser516.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class JsonMetricExporterTest {

    @Test
    void shouldCreateJsonFileInOutputDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("json-metrics-test");

        List<MetricRow> rows = List.of(
                new MetricRow(
                        MetricType.FAN_OUT,
                        Scope.CLASS,
                        "edu.asu.Calculator",
                        3));

        JsonMetricExporter exporter = new JsonMetricExporter();

        Path outputFile = exporter.export(rows, tempDir);

        assertNotNull(outputFile);
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        assertTrue(outputFile.toString().endsWith(".json"));
    }

    @Test
    void shouldProduceValidJsonStructure() throws Exception {
        Path tempDir = Files.createTempDirectory("json-metrics-test");

        List<MetricRow> rows = List.of(
                new MetricRow(
                        MetricType.FAN_OUT,
                        Scope.CLASS,
                        "edu.asu.Calculator",
                        3));

        JsonMetricExporter exporter = new JsonMetricExporter();

        Path outputFile = exporter.export(rows, tempDir);
        String content = Files.readString(outputFile);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);

        assertNotNull(root);

        assertTrue(root.has("metric"));
        assertTrue(root.has("scope"));
        assertTrue(root.has("results"));

        assertEquals("FAN_OUT", root.get("metric").asText());
        assertEquals("CLASS", root.get("scope").asText());

        JsonNode results = root.get("results");
        assertTrue(results.isArray());
        assertEquals(1, results.size());

        JsonNode first = results.get(0);
        assertTrue(first.has("entity"));
        assertTrue(first.has("value"));

        assertEquals("edu.asu.Calculator", first.get("entity").asText());
        assertEquals(3, first.get("value").asInt());
    }

    @Test
    void shouldWriteEmptyResultsArrayWhenNoMetrics() throws Exception {
        Path tempDir = Files.createTempDirectory("json-metrics-test");
        JsonMetricExporter exporter = new JsonMetricExporter();

        Path outputFile = exporter.export(List.of(), tempDir);
        String content = Files.readString(outputFile);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);

        assertTrue(root.has("results"));
        assertEquals(0, root.get("results").size());
    }

    @Test
    void shouldSerializeResultsInDescendingOrderByValue() throws Exception {
        Path tempDir = Files.createTempDirectory("json-metrics-test");

        List<MetricRow> rows = List.of(
                new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "ClassA", 2),
                new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "ClassC", 5),
                new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "ClassB", 3));

        JsonMetricExporter exporter = new JsonMetricExporter();

        Path outputFile = exporter.export(rows, tempDir);
        String content = Files.readString(outputFile);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);
        JsonNode results = root.get("results");

        assertEquals(3, results.size());

        assertEquals("ClassC", results.get(0).get("entity").asText());
        assertEquals(5, results.get(0).get("value").asInt());

        assertEquals("ClassB", results.get(1).get("entity").asText());
        assertEquals(3, results.get(1).get("value").asInt());

        assertEquals("ClassA", results.get(2).get("entity").asText());
        assertEquals(2, results.get(2).get("value").asInt());
    }
}