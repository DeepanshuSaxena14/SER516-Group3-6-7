package edu.asu.ser516.metrics;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScannerMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String inputPath = null;
        String outputPath = null;
        String format = "text";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                case "--output":
                    if (i + 1 < args.length)
                        outputPath = args[++i];
                    break;
                case "-f":
                case "--format":
                    if (i + 1 < args.length)
                        format = args[++i].toLowerCase();
                    break;
                case "-h":
                case "--help":
                    printUsage();
                    return;
                default:
                    if (inputPath == null)
                        inputPath = args[i];
                    break;
            }
        }

        if (inputPath == null) {
            System.err.println("Error: No input path specified.");
            printUsage();
            System.exit(1);
        }

        try {
            Path root = Path.of(inputPath);
            if (!root.toFile().exists()) {
                System.err.println("Error: Input path does not exist: " + inputPath);
                System.exit(1);
            }

            List<Path> files = SourceScanner.findJavaFiles(root);

            CouplingAnalyzer analyzer = new CouplingAnalyzer(files);
            analyzer.analyze();

            Map<String, Integer> fanOut = analyzer.getFanOut();
            Map<String, Integer> fanIn = analyzer.getFanIn();

            Map<String, int[]> results = new TreeMap<>();
            fanOut.forEach((k, v) -> results.computeIfAbsent(k, x -> new int[2])[0] = v);
            fanIn.forEach((k, v) -> results.computeIfAbsent(k, x -> new int[2])[1] = v);

            MethodCouplingAnalyzer methodAnalyzer = new MethodCouplingAnalyzer(files);
            methodAnalyzer.analyze();

            Map<String, Integer> methodFanOut = methodAnalyzer.getFanOut();

            String output = formatOutput(results, methodFanOut, format);

            if (outputPath != null) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
                    writer.print(output);
                }
                System.out.println("Output written to " + outputPath);
            } else {
                System.out.println(output);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar ser516-group6-metrics-1.0.0.jar <input-path> [options]");
        System.out.println("Options:");
        System.out.println("  -o, --output <file>   Specify output file");
        System.out.println("  -f, --format <fmt>    Output format: text, csv, json (default: text)");
        System.out.println("  -h, --help            Show this help message");
    }

    private static String formatOutput(Map<String, int[]> results, Map<String, Integer> methodFanOut, String format) {
        StringBuilder sb = new StringBuilder();
        switch (format) {
            case "csv":
                sb.append("Class,Fan-Out,Fan-In\n");
                results.forEach((cls, metrics) -> sb.append(String.format("%s,%d,%d\n", cls, metrics[0], metrics[1])));
                sb.append("\nMethod,Fan-Out\n");
                methodFanOut
                        .forEach((m, fanOut) -> sb.append(String.format("%s,%d\n", m, fanOut)));
                break;
            case "json":
                sb.append("[\n");
                Iterator<Map.Entry<String, int[]>> it = results.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, int[]> entry = it.next();
                    sb.append(String.format("  {\"class\": \"%s\", \"fanOut\": %d, \"fanIn\": %d}",
                            entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
                    if (it.hasNext() || !methodFanOut.isEmpty())
                        sb.append(",");
                    sb.append("\n");
                }
                Iterator<Map.Entry<String, Integer>> mit = methodFanOut.entrySet().iterator();
                while (mit.hasNext()) {
                    Map.Entry<String, Integer> entry = mit.next();
                    sb.append(String.format("  {\"method\": \"%s\", \"fanOut\": %d}",
                            entry.getKey(), entry.getValue()));
                    if (mit.hasNext())
                        sb.append(",");
                    sb.append("\n");
                }
                sb.append("]");
                break;
            case "text":
            default:
                sb.append(String.format("%-50s %-10s %-10s\n", "Class", "Fan-Out", "Fan-In"));
                sb.append("-".repeat(72)).append("\n");
                results.forEach(
                        (cls, metrics) -> sb.append(String.format("%-50s %-10d %-10d\n", cls, metrics[0], metrics[1])));
                sb.append("\n");
                sb.append(String.format("%-80s %-10s\n", "Method", "Fan-Out"));
                sb.append("-".repeat(92)).append("\n");
                methodFanOut.forEach(
                        (m, fanOut) -> sb.append(String.format("%-80s %-10d\n", m, fanOut)));
                break;
        }
        return sb.toString();
    }
}
