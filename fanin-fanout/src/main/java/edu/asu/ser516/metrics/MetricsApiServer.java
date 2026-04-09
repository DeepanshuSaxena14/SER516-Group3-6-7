package edu.asu.ser516.metrics;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MetricsApiServer {

    private MetricsApiServer() {
    }

    public static Javalin create() {
        return Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        })
                .get("/metrics/fanout", MetricsApiServer::handleFanOut)
                .get("/metrics/fanin", MetricsApiServer::handleFanIn)
                .get("/metrics/analyze", MetricsApiServer::handleAnalyze);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        create().start(port);
        System.out.println("Metrics API server started on port " + port);
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private static void handleFanOut(Context ctx) {
        Path root;
        try {
            root = validatePath(ctx);
        } catch (IllegalArgumentException e) {
            sendError(ctx, e.getMessage());
            return;
        }

        try {
            List<Path> javaFiles = SourceScanner.findJavaFiles(root);
            Map<String, Set<String>> outgoing = OutgoingReferenceExtractor.extractOutgoingRefs(javaFiles);
            List<ClassReference> edges = ReferenceAdapters.toEdges(outgoing);
            Map<String, Integer> fanOut = FanOutComputer.computeFanOut(edges);

            Map<String, Integer> sorted = fanOut.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));

            ctx.json(toFanOutJsonArray(sorted));

            // Persist to Supabase for Grafana
            MetricDbWriter.writeFanOut(sorted);

        } catch (IOException e) {
            sendError(ctx, "Failed to scan project at path: " + root + " — " + e.getMessage());
        }
    }

    private static void handleFanIn(Context ctx) {
        Path root;
        try {
            root = validatePath(ctx);
        } catch (IllegalArgumentException e) {
            sendError(ctx, e.getMessage());
            return;
        }

        try {
            List<Path> javaFiles = SourceScanner.findJavaFiles(root);

            // Class-level Fan-In via CouplingAnalyzer
            CouplingAnalyzer classAnalyzer = new CouplingAnalyzer(javaFiles);
            classAnalyzer.analyze();
            Map<String, Integer> classLevelFanIn = classAnalyzer.getFanIn()
                    .entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));

            // Method-level Fan-In via MethodCouplingAnalyzer
            MethodCouplingAnalyzer methodAnalyzer = new MethodCouplingAnalyzer(javaFiles);
            methodAnalyzer.analyze();
            Map<String, Integer> methodLevelFanIn = methodAnalyzer.getFanIn()
                    .entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));

            ctx.json(toUnifiedFanInJson(classLevelFanIn, methodLevelFanIn));

            // Persist to Supabase for Grafana
            MetricDbWriter.writeFanIn(classLevelFanIn);

        } catch (IOException e) {
            sendError(ctx, "Failed to scan project at path: " + root + " — " + e.getMessage());
        }
    }

    /**
     * New endpoint: accepts a GitHub URL, clones the repo, runs fan-in/fan-out,
     * persists results to Supabase, and returns a summary.
     *
     * Usage: GET /metrics/analyze?github_link=https://github.com/owner/repo
     */
    private static void handleAnalyze(Context ctx) {
        String repoUrl = ctx.queryParam("github_link");

        if (repoUrl == null || repoUrl.isBlank()) {
            ctx.status(400);
            sendError(ctx, "Required query parameter 'github_link' is missing. " +
                    "Usage: /metrics/analyze?github_link=https://github.com/owner/repo");
            return;
        }

        try {
            // Step 1 — Clone the repository
            Path root = RepoCloner.cloneRepo(repoUrl);

            // Step 2 — Find all Java files
            List<Path> javaFiles = SourceScanner.findJavaFiles(root);

            if (javaFiles.isEmpty()) {
                sendError(ctx, "No Java files found in repository: " + repoUrl);
                return;
            }

            // Step 3 — Compute Fan-Out
            Map<String, Set<String>> outgoing = OutgoingReferenceExtractor.extractOutgoingRefs(javaFiles);
            List<ClassReference> edges = ReferenceAdapters.toEdges(outgoing);
            Map<String, Integer> fanOut = FanOutComputer.computeFanOut(edges)
                    .entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue,
                            (e1, e2) -> e1, LinkedHashMap::new));

            // Step 4 — Compute Fan-In
            CouplingAnalyzer classAnalyzer = new CouplingAnalyzer(javaFiles);
            classAnalyzer.analyze();
            Map<String, Integer> fanIn = classAnalyzer.getFanIn()
                    .entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue,
                            (e1, e2) -> e1, LinkedHashMap::new));

            // Step 5 — Persist to Supabase for Grafana
            MetricDbWriter.writeFanOut(fanOut);
            MetricDbWriter.writeFanIn(fanIn);

            // Step 6 — Return summary
            ctx.contentType("application/json");
            ctx.result("{" +
                    "\"status\":\"ok\"," +
                    "\"repo\":\"" + jsonEscape(repoUrl) + "\"," +
                    "\"javaFilesAnalyzed\":" + javaFiles.size() + "," +
                    "\"classesWithFanOut\":" + fanOut.size() + "," +
                    "\"classesWithFanIn\":" + fanIn.size() +
                    "}");

        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Analysis failed for repo: " + repoUrl + " — " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Path validatePath(Context ctx) {
        String pathParam = ctx.queryParam("path");

        if (pathParam == null || pathParam.isBlank()) {
            ctx.status(400);
            throw new IllegalArgumentException(
                    "Required query parameter 'path' is missing or empty. " +
                            "Usage: /metrics/fanout?path=/absolute/path/to/java/project");
        }

        Path root = Path.of(pathParam);

        if (!Files.exists(root)) {
            ctx.status(400);
            throw new IllegalArgumentException("Path does not exist: " + pathParam);
        }

        if (!Files.isDirectory(root)) {
            ctx.status(400);
            throw new IllegalArgumentException("Path is not a directory: " + pathParam);
        }

        return root;
    }

    private static void sendError(Context ctx, String message) {
        ctx.status(400);
        ctx.contentType("application/json");
        ctx.result("{\"error\":\"" + jsonEscape(message) + "\"}");
    }

    private static String toFanOutJsonArray(Map<String, Integer> fanOut) {
        StringBuilder sb = new StringBuilder("[\n");
        int i = 0, n = fanOut.size();
        for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
            sb.append("  {\"class\":\"")
                    .append(jsonEscape(e.getKey()))
                    .append("\",\"fanOut\":")
                    .append(e.getValue())
                    .append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toUnifiedFanInJson(Map<String, Integer> classLevel,
            Map<String, Integer> methodLevel) {
        StringBuilder sb = new StringBuilder("{\n");

        sb.append("  \"classLevel\": [\n");
        int i = 0, n = classLevel.size();
        for (Map.Entry<String, Integer> e : classLevel.entrySet()) {
            sb.append("    {\"class\":\"")
                    .append(jsonEscape(e.getKey()))
                    .append("\",\"fanIn\":")
                    .append(e.getValue())
                    .append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"methodLevel\": [\n");
        i = 0;
        n = methodLevel.size();
        for (Map.Entry<String, Integer> e : methodLevel.entrySet()) {
            sb.append("    {\"method\":\"")
                    .append(jsonEscape(e.getKey()))
                    .append("\",\"fanIn\":")
                    .append(e.getValue())
                    .append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");

        return sb.toString();
    }

    private static String jsonEscape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}