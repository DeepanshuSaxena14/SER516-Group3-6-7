package edu.asu.ser516.metrics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CsvMetricExporter {

    private static final String HEADER = "metricType,scope,entity,value,packageName,filePath";

    public Path export(List<MetricRow> rows, Path outDir) throws IOException {
        Objects.requireNonNull(outDir, "outDir");

        Path outFile = ExportFileManager.prepareOutputFile(
                outDir,
                "metrics",
                "csv");

        List<String> lines = new ArrayList<>();
        lines.add(HEADER);

        List<MetricRow> safeRows = (rows == null) ? Collections.emptyList() : rows;
        List<MetricRow> sorted = MetricRowSorter.sort(safeRows);

        for (MetricRow r : sorted) {
            lines.add(toCsvLine(r));
        }

        String content = String.join(System.lineSeparator(), lines);
        ExportFileManager.atomicWrite(outFile, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return outFile;
    }

    private static String toCsvLine(MetricRow r) {
        Objects.requireNonNull(r, "MetricRow");

        return String.join(",",
                csvEscape(r.getMetricType().name()),
                csvEscape(r.getScope().name()),
                csvEscape(r.getEntity()),
                csvEscape(String.valueOf(r.getValue())),
                csvEscape(nullToEmpty(r.getPackageName())),
                csvEscape(nullToEmpty(r.getFilePath())));
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "";
        }

        boolean needsQuotes =
                s.contains(",") ||
                s.contains("\"") ||
                s.contains("\n") ||
                s.contains("\r");

        String escaped = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}