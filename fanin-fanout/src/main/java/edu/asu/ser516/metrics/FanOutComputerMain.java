package edu.asu.ser516.metrics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FanOutComputerMain {

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println(
                    "Usage: FanOutComputerMain <path-to-java-project> [csv|json|both|none] [outDir]");
            System.exit(1);
        }

        Path root = Path.of(args[0]);
        String format = (args.length >= 2) ? args[1].toLowerCase() : "none";
        Path outDir = (args.length >= 3) ? Path.of(args[2]) : Path.of("out");

        // 1) Scan
        List<Path> javaFiles = SourceScanner.findJavaFiles(root);

        // 2) Extract outgoing refs
        Map<String, Set<String>> outgoing = OutgoingReferenceExtractor.extractOutgoingRefs(javaFiles);

        // 3) Convert to edges
        List<ClassReference> edges = ReferenceAdapters.toEdges(outgoing);

        // 4) Compute fan-out
        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(edges);

        // 5) Sort descending
        Map<String, Integer> fanOutSorted = new LinkedHashMap<>();
        fanOut.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> fanOutSorted.put(e.getKey(), e.getValue()));

        // 6) Print fan-out
        System.out.println("Class-level Fan-Out:");
        fanOutSorted.forEach((cls, val) -> System.out.println(cls + " -> " + val));

        // 7) Compute fan-in
        CouplingAnalyzer analyzer = new CouplingAnalyzer(javaFiles);
        analyzer.analyze();
        Map<String, Integer> fanInSorted = analyzer.getFanIn()
                .entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

        System.out.println("\nClass-level Fan-In:");
        fanInSorted.forEach((cls, val) -> System.out.println(cls + " -> " + val));

        // 8) Optional CSV / JSON output
        if (!format.equals("none")) {

            Files.createDirectories(outDir);

            if (format.equals("csv") || format.equals("both")) {
                Path csv = outDir.resolve("fanout.csv");
                MetricWriters.writeFanOutCsv(fanOutSorted, csv);
                System.out.println("Wrote: " + csv.toAbsolutePath());

                Path fanInCsv = outDir.resolve("fanin.csv");
                MetricWriters.writeFanInCsv(fanInSorted, fanInCsv);
                System.out.println("Wrote: " + fanInCsv.toAbsolutePath());
            }

            if (format.equals("json") || format.equals("both")) {
                Path json = outDir.resolve("fanout.json");
                MetricWriters.writeFanOutJson(fanOutSorted, json);
                System.out.println("Wrote: " + json.toAbsolutePath());

                Path fanInJson = outDir.resolve("fanin.json");
                MetricWriters.writeFanInJson(fanInSorted, fanInJson);
                System.out.println("Wrote: " + fanInJson.toAbsolutePath());
            }
        }

        // 9) Optional: write to PostgreSQL for Grafana (when JDBC_URL is set)
        MetricDbWriter.writeFanOut(fanOutSorted);
        MetricDbWriter.writeFanIn(fanInSorted);
    }
}