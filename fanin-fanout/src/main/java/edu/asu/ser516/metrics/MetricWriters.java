package edu.asu.ser516.metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class MetricWriters {

    private MetricWriters() {}

    public static void writeFanOutCsv(Map<String, Integer> fanOut, Path outFile)
            throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(outFile)) {
            w.write("class,fanOut");
            w.newLine();

            for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
                w.write(csvEscape(e.getKey()));
                w.write(",");
                w.write(Integer.toString(e.getValue()));
                w.newLine();
            }
        }
    }

    public static void writeFanOutJson(Map<String, Integer> fanOut, Path outFile)
            throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(outFile)) {
            w.write("[\n");

            int i = 0;
            int n = fanOut.size();

            for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
                w.write("  {\"class\":\"");
                w.write(jsonEscape(e.getKey()));
                w.write("\",\"fanOut\":");
                w.write(Integer.toString(e.getValue()));
                w.write("}");

                if (++i < n) {
                    w.write(",");
                }

                w.write("\n");
            }

            w.write("]\n");
        }
    }

    public static void writeFanInCsv(Map<String, Integer> fanIn, Path outFile)
            throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(outFile)) {
            w.write("class,fanIn");
            w.newLine();

            for (Map.Entry<String, Integer> e : fanIn.entrySet()) {
                w.write(csvEscape(e.getKey()));
                w.write(",");
                w.write(Integer.toString(e.getValue()));
                w.newLine();
            }
        }
    }

    public static void writeFanInJson(Map<String, Integer> fanIn, Path outFile)
            throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(outFile)) {
            w.write("[\n");

            int i = 0;
            int n = fanIn.size();

            for (Map.Entry<String, Integer> e : fanIn.entrySet()) {
                w.write("  {\"class\":\"");
                w.write(jsonEscape(e.getKey()));
                w.write("\",\"fanIn\":");
                w.write(Integer.toString(e.getValue()));
                w.write("}");

                if (++i < n) {
                    w.write(",");
                }

                w.write("\n");
            }

            w.write("]\n");
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";

        boolean needsQuotes =
                s.contains(",") ||
                s.contains("\"") ||
                s.contains("\n") ||
                s.contains("\r");

        String t = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + t + "\"" : t;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";

        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
    }
}