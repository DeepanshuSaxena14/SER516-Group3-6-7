package edu.asu.ser516.metrics;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MetricsApiServer {

    private MetricsApiServer() {
    }

    // -------------------------------------------------------------------------
    // Scope enum
    // -------------------------------------------------------------------------

    private enum Scope {
        CLASS, PACKAGE, PROJECT
    }

    // -------------------------------------------------------------------------
    // Server setup
    // -------------------------------------------------------------------------

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

        String scope;
        try {
            scope = resolveFanOutScope(ctx);
        } catch (IllegalArgumentException e) {
            sendError(ctx, e.getMessage());
            return;
        }

        try {
            List<Path> javaFiles = SourceScanner.findJavaFiles(root);
            Map<String, Set<String>> outgoing = OutgoingReferenceExtractor.extractOutgoingRefs(javaFiles);
            List<ClassReference> edges = ReferenceAdapters.toEdges(outgoing);
            Map<String, Integer> classFanOut = FanOutComputer.computeFanOut(edges);

            Map<String, Integer> sorted;
            String jsonBody;
            String dbScope;

            switch (scope) {
                case "package" -> {
                    sorted = sortDescending(aggregateFanOutByPackage(classFanOut));
                    jsonBody = toPackageFanOutJsonArray(sorted);
                    dbScope = "package";
                }
                case "project" -> {
                    sorted = sortDescending(aggregateFanOutByProject(classFanOut, root));
                    jsonBody = toProjectFanOutJsonArray(sorted);
                    dbScope = "project";
                }
                default -> {
                    sorted = sortDescending(classFanOut);
                    jsonBody = toFanOutJsonArray(sorted);
                    dbScope = "class";
                }
            }

            MetricDbWriter.writeFanOut(sorted);
            ctx.json(jsonBody);

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

        Scope scope;
        try {
            scope = validateScope(ctx);
        } catch (IllegalArgumentException e) {
            sendError(ctx, e.getMessage());
            return;
        }

        try {
            List<Path> javaFiles = SourceScanner.findJavaFiles(root);

            switch (scope) {
                case CLASS -> {
                    CouplingAnalyzer classAnalyzer = new CouplingAnalyzer(javaFiles);
                    classAnalyzer.analyze();
                    Map<String, Integer> classLevel = sortDescending(classAnalyzer.getFanIn());
                    MetricDbWriter.writeFanIn(classLevel);
                    ctx.json(toFanInJsonArray(classLevel, "class"));
                }
                case PACKAGE -> {
                    CouplingAnalyzer classAnalyzer = new CouplingAnalyzer(javaFiles);
                    classAnalyzer.analyze();
                    Map<String, Integer> packageLevel = classAnalyzer.getFanIn()
                            .entrySet().stream()
                            .collect(Collectors.groupingBy(
                                    e -> {
                                        String fqn = e.getKey();
                                        int dot = fqn.lastIndexOf('.');
                                        return dot >= 0 ? fqn.substring(0, dot) : "(default)";
                                    },
                                    Collectors.summingInt(Map.Entry::getValue)))
                            .entrySet().stream()
                            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (e1, e2) -> e1,
                                    LinkedHashMap::new));
                    ctx.json(toFanInJsonArray(packageLevel, "package"));
                }
                case PROJECT -> {
                    CouplingAnalyzer classAnalyzer = new CouplingAnalyzer(javaFiles);
                    classAnalyzer.analyze();
                    int total = classAnalyzer.getFanIn().values().stream()
                            .mapToInt(Integer::intValue).sum();
                    ctx.json("{\"scope\":\"project\",\"totalFanIn\":" + total + "}");
                }
            }

        } catch (IOException e) {
            sendError(ctx, "Failed to scan project at path: " + root + " — " + e.getMessage());
        }
    }

    /**
     * GET /metrics/analyze?github_link=https://github.com/owner/repo.git
     *
     * Clones the given public GitHub repository, runs Fan-Out and Fan-In analysis,
     * persists results to Supabase, and returns a JSON summary.
     */
    private static void handleAnalyze(Context ctx) {
        String repoUrl = ctx.queryParam("github_link");

        if (repoUrl == null || repoUrl.isBlank()) {
            ctx.status(400);
            sendError(ctx, "Required query parameter 'github_link' is missing. " +
                    "Usage: /metrics/analyze?github_link=https://github.com/owner/repo.git");
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
            Map<String, Integer> fanOut = sortDescending(FanOutComputer.computeFanOut(edges));

            // Step 4 — Compute Fan-In
            CouplingAnalyzer classAnalyzer = new CouplingAnalyzer(javaFiles);
            classAnalyzer.analyze();
            Map<String, Integer> fanIn = sortDescending(classAnalyzer.getFanIn());

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
    // Aggregation helpers
    // -------------------------------------------------------------------------

    private static Map<String, Integer> sortDescending(Map<String, Integer> map) {
        return map.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    private static Map<String, Integer> aggregateFanOutByPackage(Map<String, Integer> classFanOut) {
        Map<String, Integer> packageFanOut = new HashMap<>();
        for (Map.Entry<String, Integer> entry : classFanOut.entrySet()) {
            String fqcn = entry.getKey();
            String packageName = fqcn.contains(".")
                    ? fqcn.substring(0, fqcn.lastIndexOf('.'))
                    : "(default)";
            packageFanOut.merge(packageName, entry.getValue(), Integer::sum);
        }
        return packageFanOut;
    }

    private static Map<String, Integer> aggregateFanOutByProject(Map<String, Integer> classFanOut, Path root) {
        Path fileName = root.getFileName();
        String name = (fileName != null && !fileName.toString().isBlank())
                ? fileName.toString()
                : root.toAbsolutePath().toString();
        int total = classFanOut.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put(name, total);
        return m;
    }

    // -------------------------------------------------------------------------
    // Validation helpers
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

    private static String resolveFanOutScope(Context ctx) {
        String scopeParam = ctx.queryParam("scope");
        if (scopeParam == null || scopeParam.isBlank()) {
            return "class"; // default
        }
        String s = scopeParam.trim().toLowerCase();
        return switch (s) {
            case "class", "package", "project" -> s;
            default -> {
                ctx.status(400);
                throw new IllegalArgumentException(
                        "Invalid query parameter 'scope'. Allowed values: class, package, project.");
            }
        };
    }

    private static Scope validateScope(Context ctx) {
        String scopeParam = ctx.queryParam("scope");
        if (scopeParam == null || scopeParam.isBlank()) {
            return Scope.CLASS; // default
        }
        try {
            return Scope.valueOf(scopeParam.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            throw new IllegalArgumentException(
                    "Invalid scope '" + scopeParam + "'. Valid values are: class, package, project.");
        }
    }

    // -------------------------------------------------------------------------
    // JSON serialization helpers
    // -------------------------------------------------------------------------

    private static String toFanOutJsonArray(Map<String, Integer> fanOut) {
        StringBuilder sb = new StringBuilder("[\n");
        int i = 0, n = fanOut.size();
        for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
            sb.append("  {\"class\":\"").append(jsonEscape(e.getKey()))
                    .append("\",\"fanOut\":").append(e.getValue()).append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toPackageFanOutJsonArray(Map<String, Integer> fanOut) {
        StringBuilder sb = new StringBuilder("[\n");
        int i = 0, n = fanOut.size();
        for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
            sb.append("  {\"package\":\"").append(jsonEscape(e.getKey()))
                    .append("\",\"fanOut\":").append(e.getValue()).append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toProjectFanOutJsonArray(Map<String, Integer> fanOut) {
        StringBuilder sb = new StringBuilder("[\n");
        int i = 0, n = fanOut.size();
        for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
            sb.append("  {\"project\":\"").append(jsonEscape(e.getKey()))
                    .append("\",\"fanOut\":").append(e.getValue()).append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toFanInJsonArray(Map<String, Integer> fanIn, String labelKey) {
        StringBuilder sb = new StringBuilder("[\n");
        int i = 0, n = fanIn.size();
        for (Map.Entry<String, Integer> e : fanIn.entrySet()) {
            sb.append("  {\"").append(labelKey).append("\":\"")
                    .append(jsonEscape(e.getKey()))
                    .append("\",\"fanIn\":").append(e.getValue()).append("}");
            if (++i < n)
                sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void sendError(Context ctx, String message) {
        ctx.status(400);
        ctx.contentType("application/json");
        ctx.result("{\"error\":\"" + jsonEscape(message) + "\"}");
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