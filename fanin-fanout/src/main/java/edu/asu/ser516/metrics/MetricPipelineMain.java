package edu.asu.ser516.metrics;

import java.nio.file.Path;
import java.util.*;

public class MetricPipelineMain {

    public static void main(String[] args) throws Exception {

        String inputPathStr = resolveConfig("INPUT_PATH",  args, 0, null);
        String outDirStr    = resolveConfig("OUTPUT_DIR",  args, 1, "metrics-output");

        if (inputPathStr == null || inputPathStr.isBlank()) {
            System.err.println("ERROR: inputPath is required.");
            System.err.println("  Usage: java -jar app.jar <inputPath> [outputDir]");
            System.err.println("  Or set env var: INPUT_PATH=/path/to/project");
            System.exit(1);
        }

        Path inputPath = Path.of(inputPathStr);
        Path outDir    = Path.of(outDirStr);

        if (!inputPath.toFile().exists()) {
            System.err.println("ERROR: inputPath does not exist: " + inputPath.toAbsolutePath());
            System.exit(2);
        }

        if (!inputPath.toFile().isDirectory()) {
            System.err.println("ERROR: inputPath is not a directory: " + inputPath.toAbsolutePath());
            System.exit(2);
        }

        System.out.println("Scanning: " + inputPath.toAbsolutePath());

        List<Path> javaFiles = SourceScanner.findJavaFiles(inputPath);
        System.out.println("Found " + javaFiles.size() + " Java files.");

        Map<String, Set<String>> outgoing =
                OutgoingReferenceExtractor.extractOutgoingRefs(javaFiles);
        List<ClassReference> edges = ReferenceAdapters.toEdges(outgoing);
        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(edges);

        MethodFanInAnalyzer methodFanInAnalyzer = new MethodFanInAnalyzer();
        Map<String, Integer> methodFanIn = methodFanInAnalyzer.computeFanIn(javaFiles);

        List<MetricRow> rows = new ArrayList<>();
        fanOut.forEach((entity, value) ->
                rows.add(new MetricRow(MetricType.FAN_OUT, Scope.CLASS,
                        entity, value, extractPackage(entity), null)));

        methodFanIn.forEach((entity, value) ->
                rows.add(new MetricRow(MetricType.FAN_IN, Scope.METHOD,
                        entity, value, extractPackageFromMethodKey(entity), null)));

        CsvMetricExporter  csvExporter  = new CsvMetricExporter();
        JsonMetricExporter jsonExporter = new JsonMetricExporter();

        Path csvFile  = csvExporter.export(rows, outDir);
        Path jsonFile = jsonExporter.export(rows, outDir);

        System.out.println("CSV  written: " + csvFile.toAbsolutePath());
        System.out.println("JSON written: " + jsonFile.toAbsolutePath());
    }

    private static String extractPackage(String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        return (lastDot > 0) ? fqcn.substring(0, lastDot) : "";
    }

    private static String extractPackageFromMethodKey(String methodKey) {
        int parenIndex = methodKey.indexOf('(');
        String withoutParams = (parenIndex >= 0) ? methodKey.substring(0, parenIndex) : methodKey;
        int lastDot = withoutParams.lastIndexOf('.');
        if (lastDot <= 0) {
            return "";
        }
        String classAndPackage = withoutParams.substring(0, lastDot);
        int classDot = classAndPackage.lastIndexOf('.');
        return (classDot > 0) ? classAndPackage.substring(0, classDot) : "";
    }

    private static String resolveConfig(String envKey, String[] args,
                                        int argIndex, String defaultValue) {
        if (args.length > argIndex && args[argIndex] != null
                && !args[argIndex].isBlank()) {
            return args[argIndex];
        }
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return defaultValue;
    }
}