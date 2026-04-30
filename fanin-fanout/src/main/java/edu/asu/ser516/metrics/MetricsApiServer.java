package edu.asu.ser516.metrics;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class MetricsApiServer {

    private MetricsApiServer() {
    }

    private static final PrometheusMeterRegistry PROMETHEUS_REGISTRY = new PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT);

    static {
        // Bind standard JVM metrics: memory, GC, threads, CPU, class loading
        new JvmMemoryMetrics().bindTo(PROMETHEUS_REGISTRY);
        new JvmGcMetrics().bindTo(PROMETHEUS_REGISTRY);
        new JvmThreadMetrics().bindTo(PROMETHEUS_REGISTRY);
        new ClassLoaderMetrics().bindTo(PROMETHEUS_REGISTRY);
        new ProcessorMetrics().bindTo(PROMETHEUS_REGISTRY);
    }
    private static final String TAIGA_SERVICE_URL = System.getenv().getOrDefault("TAIGA_SERVICE_URL",
            "http://taiga-service:8080");

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
                .get("/prometheus", MetricsApiServer::handlePrometheus)
                .get("/metrics/fanout", MetricsApiServer::handleFanOut)
                .get("/metrics/fanin", MetricsApiServer::handleFanIn)
                .get("/metrics/analyze", MetricsApiServer::handleAnalyze)
                .get("/metrics/fanin/methods", MetricsApiServer::handleFanInMethods)
                .get("/metrics/taiga/auc", MetricsApiServer::handleTaigaAuc)
                .get("/metrics/taiga/focus-factor", MetricsApiServer::handleTaigaFocusFactor)
                .get("/taiga/stories", MetricsApiServer::handleTaigaStories)
                .get("/taiga/sprint", MetricsApiServer::handleTaigaSprint)
                .get("/health", ctx -> ctx.result("OK"));
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        create().start(port);
        System.out.println("Metrics API server started on port " + port);
        System.out.println("Prometheus metrics available at http://localhost:" + port + "/prometheus");
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private static void handlePrometheus(Context ctx) {
        ctx.contentType("text/plain; version=0.0.4; charset=utf-8");
        ctx.result(PROMETHEUS_REGISTRY.scrape());
    }

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

    private static void handleFanInMethods(Context ctx) {
        Path root;
        try {
            root = validatePath(ctx);
        } catch (IllegalArgumentException e) {
            sendError(ctx, e.getMessage());
            return;
        }

        try {
            List<Path> javaFiles = SourceScanner.findJavaFiles(root);
            MethodCouplingAnalyzer methodAnalyzer = new MethodCouplingAnalyzer(javaFiles);
            methodAnalyzer.analyze();
            Map<String, Integer> methodFanIn = sortDescending(methodAnalyzer.getFanIn());
            ctx.json(toFanInJsonArray(methodFanIn, "method"));
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

    /**
     * GET /metrics/taiga/auc?project_id=&lt;int&gt;&amp;sprint_id=&lt;int&gt;
     *
     * Authenticates against the Taiga API using credentials supplied via the
     * {@code TAIGA_USERNAME} / {@code TAIGA_PASSWORD} environment variables,
     * fetches user stories for the requested sprint, computes the Area Under
     * the Curve (AUC) metric via {@link AucService}, and returns a JSON
     * summary containing {@code aucWork}, {@code aucValue}, and
     * {@code aucRatio}.
     */
    private static void handleTaigaAuc(Context ctx) {
        String projectIdParam = ctx.queryParam("project_id");
        String sprintIdParam = ctx.queryParam("sprint_id");

        int projectId, sprintId;
        try {
            if (projectIdParam == null || projectIdParam.isBlank())
                throw new NumberFormatException("missing");
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Query parameter 'project_id' is required and must be an integer.");
            return;
        }
        try {
            if (sprintIdParam == null || sprintIdParam.isBlank())
                throw new NumberFormatException("missing");
            sprintId = Integer.parseInt(sprintIdParam.trim());
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Query parameter 'sprint_id' is required and must be an integer.");
            return;
        }

        try {
            // --- Fetch sprint dates from taiga-service ---
            HttpRequest sprintReq = HttpRequest.newBuilder()
                    .uri(URI.create(TAIGA_SERVICE_URL + "/taiga/sprint?sprint_id=" + sprintId))
                    .GET().build();
            HttpResponse<String> sprintResp = HTTP.send(sprintReq, HttpResponse.BodyHandlers.ofString());
            if (sprintResp.statusCode() != 200) {
                ctx.status(502);
                sendError(ctx, "taiga-service sprint fetch failed: " + sprintResp.body());
                return;
            }
            Map<String, Object> sprintData = MAPPER.readValue(sprintResp.body(), new TypeReference<>() {
            });
            String sprintStart = (String) sprintData.get("sprintStart");
            String sprintEnd = (String) sprintData.get("sprintEnd");

            if (sprintStart == null || sprintStart.isBlank()) {
                ctx.status(422);
                sendError(ctx, "Could not retrieve sprint start date for sprint_id=" + sprintId);
                return;
            }
            if (sprintEnd == null || sprintEnd.isBlank()) {
                ctx.status(422);
                sendError(ctx, "Could not retrieve sprint end date for sprint_id=" + sprintId);
                return;
            }

            // --- Fetch stories from taiga-service ---
            HttpRequest storiesReq = HttpRequest.newBuilder()
                    .uri(URI.create(
                            TAIGA_SERVICE_URL + "/taiga/stories?project_id=" + projectId + "&sprint_id=" + sprintId))
                    .GET().build();
            HttpResponse<String> storiesResp = HTTP.send(storiesReq, HttpResponse.BodyHandlers.ofString());
            if (storiesResp.statusCode() != 200) {
                ctx.status(502);
                sendError(ctx, "taiga-service stories fetch failed: " + storiesResp.body());
                return;
            }
            List<Map<String, Object>> stories = MAPPER.readValue(storiesResp.body(), new TypeReference<>() {
            });

            // --- Compute AUC ---
            AucService.AucResult auc = AucService.compute(stories, sprintStart, sprintEnd);

            // --- Return JSON response ---
            ctx.contentType("application/json");
            ctx.result("{"
                    + "\"projectId\":" + projectId + ","
                    + "\"sprintId\":" + sprintId + ","
                    + "\"sprintStart\":\"" + jsonEscape(sprintStart) + "\","
                    + "\"sprintEnd\":\"" + jsonEscape(sprintEnd) + "\","
                    + "\"aucWork\":" + auc.aucWork() + ","
                    + "\"aucValue\":" + auc.aucValue() + ","
                    + "\"aucRatio\":" + auc.aucRatio()
                    + "}");

        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Taiga AUC computation failed: " + e.getMessage());
        }
    }

    /**
     * GET /metrics/taiga/focus-factor?project_id=&lt;int&gt;
     *
     * Fetches all sprints for the given project, computes focus factor
     * (velocity / work capacity) per sprint, and returns a JSON array.
     */
    private static void handleTaigaFocusFactor(Context ctx) {
        String projectIdParam = ctx.queryParam("project_id");

        if (projectIdParam == null || projectIdParam.isBlank()) {
            ctx.status(400);
            sendError(ctx, "Query parameter 'project_id' is required and must be an integer.");
            return;
        }

        int projectId;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (NumberFormatException e) {
            ctx.status(400);
            sendError(ctx, "Query parameter 'project_id' is required and must be an integer.");
            return;
        }

        try {
            // fetch all sprints for the project
            HttpRequest sprintsReq = HttpRequest.newBuilder()
                    .uri(URI.create(TAIGA_SERVICE_URL + "/taiga/sprints?project_id=" + projectId))
                    .GET().build();
            HttpResponse<String> sprintsResp = HTTP.send(sprintsReq, HttpResponse.BodyHandlers.ofString());
            if (sprintsResp.statusCode() != 200) {
                ctx.status(502);
                sendError(ctx, "taiga-service sprints fetch failed: " + sprintsResp.body());
                return;
            }
            List<Map<String, Object>> sprints = MAPPER.readValue(sprintsResp.body(), new TypeReference<>() {
            });

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < sprints.size(); i++) {
                Map<String, Object> sprint = sprints.get(i);
                int sprintId = ((Number) sprint.get("id")).intValue();
                String sprintName = String.valueOf(sprint.get("name"));
                String sprintStart = sprint.get("estimated_start") != null ? sprint.get("estimated_start").toString()
                        : "";
                String sprintEnd = sprint.get("estimated_finish") != null ? sprint.get("estimated_finish").toString()
                        : "";

                // fetch stories for this sprint
                HttpRequest storiesReq = HttpRequest.newBuilder()
                        .uri(URI.create(TAIGA_SERVICE_URL + "/taiga/stories?project_id=" + projectId + "&sprint_id="
                                + sprintId))
                        .GET().build();
                HttpResponse<String> storiesResp = HTTP.send(storiesReq, HttpResponse.BodyHandlers.ofString());
                List<Map<String, Object>> stories = storiesResp.statusCode() == 200
                        ? MAPPER.readValue(storiesResp.body(), new TypeReference<>() {
                        })
                        : List.of();

                FocusFactorService.FocusFactorResult result = FocusFactorService.compute(
                        sprintId, sprintName, sprintStart, sprintEnd, stories);

                if (i > 0)
                    sb.append(",");
                sb.append("{")
                        .append("\"sprintId\":").append(result.sprintId()).append(",")
                        .append("\"sprintName\":\"").append(jsonEscape(result.sprintName())).append("\",")
                        .append("\"sprintStart\":\"").append(jsonEscape(result.sprintStart())).append("\",")
                        .append("\"sprintEnd\":\"").append(jsonEscape(result.sprintEnd())).append("\",")
                        .append("\"velocity\":").append(result.velocity()).append(",")
                        .append("\"workCapacity\":").append(result.workCapacity()).append(",")
                        .append("\"focusFactor\":").append(result.focusFactor())
                        .append("}");
            }
            sb.append("]");

            ctx.contentType("application/json");
            ctx.result(sb.toString());

        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Focus factor computation failed: " + e.getMessage());
        }
    }

    private static void handleTaigaStories(Context ctx) {
        String projectId = ctx.queryParam("project_id");
        String sprintId = ctx.queryParam("sprint_id");
        if (projectId == null || sprintId == null) {
            sendError(ctx, "project_id and sprint_id are required query parameters.");
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(
                            TAIGA_SERVICE_URL + "/taiga/stories?project_id=" + projectId + "&sprint_id=" + sprintId))
                    .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            ctx.status(resp.statusCode()).contentType("application/json").result(resp.body());
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Failed to proxy stories request to taiga-service: " + e.getMessage());
        }
    }

    private static void handleTaigaSprint(Context ctx) {
        String sprintId = ctx.queryParam("sprint_id");
        if (sprintId == null) {
            sendError(ctx, "sprint_id is a required query parameter.");
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TAIGA_SERVICE_URL + "/taiga/sprint?sprint_id=" + sprintId))
                    .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            ctx.status(resp.statusCode()).contentType("application/json").result(resp.body());
        } catch (Exception e) {
            ctx.status(500);
            sendError(ctx, "Failed to proxy sprint request to taiga-service: " + e.getMessage());
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