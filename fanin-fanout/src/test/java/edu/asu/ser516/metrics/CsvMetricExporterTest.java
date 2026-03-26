package edu.asu.ser516.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvMetricExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void createsCsvFileInOutputDir() throws Exception {
        CsvMetricExporter exporter = new CsvMetricExporter();

        Path outFile = exporter.export(List.of(), tempDir);

        assertNotNull(outFile);
        assertTrue(Files.exists(outFile), "Expected CSV file to be created");
        assertTrue(outFile.toString().endsWith(".csv"));
    }

    @Test
    void writesHeaderAsFirstLine() throws Exception {
        CsvMetricExporter exporter = new CsvMetricExporter();

        Path outFile = exporter.export(List.of(), tempDir);

        String firstLine = Files.readAllLines(outFile).get(0);

        assertEquals("metricType,scope,entity,value,packageName,filePath", firstLine);
    }

    @Test
    void writesOneRowCorrectly() throws Exception {
        CsvMetricExporter exporter = new CsvMetricExporter();

        MetricRow row = new MetricRow(
                MetricType.FAN_OUT,
                Scope.CLASS,
                "simplejavacalculator.UI",
                13,
                "simplejavacalculator",
                "UI.java");

        Path outFile = exporter.export(List.of(row), tempDir);

        List<String> lines = Files.readAllLines(outFile);

        assertEquals(2, lines.size(), "Expected header + 1 data row");
        assertEquals("FAN_OUT,CLASS,simplejavacalculator.UI,13,simplejavacalculator,UI.java", lines.get(1));
    }

    @Test
    void emptyListProducesHeaderOnly() throws Exception {
        CsvMetricExporter exporter = new CsvMetricExporter();

        Path outFile = exporter.export(List.of(), tempDir);

        List<String> lines = Files.readAllLines(outFile);

        assertEquals(1, lines.size(), "Expected only header line for empty input");
        assertEquals("metricType,scope,entity,value,packageName,filePath", lines.get(0));
    }

    @Test
    void writesRowsInSortedOrder() throws Exception {
        CsvMetricExporter exporter = new CsvMetricExporter();

        MetricRow a10 = new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "A", 10, null, null);
        MetricRow b10 = new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "B", 10, null, null);
        MetricRow c20 = new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "C", 20, null, null);
        MetricRow d5 = new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "D", 5, null, null);

        Path outFile = exporter.export(List.of(a10, d5, b10, c20), tempDir);

        List<String> lines = Files.readAllLines(outFile);

        assertEquals(5, lines.size());

        assertEquals("FAN_OUT,CLASS,C,20,,", lines.get(1));
        assertEquals("FAN_OUT,CLASS,A,10,,", lines.get(2));
        assertEquals("FAN_OUT,CLASS,B,10,,", lines.get(3));
        assertEquals("FAN_OUT,CLASS,D,5,,", lines.get(4));
    }
}