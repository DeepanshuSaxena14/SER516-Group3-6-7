package org.service;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.github.CloneObject;
import org.metrics.MetricDbWriter;
import java.util.List;
import java.util.Map;

public class AeApiServer {

    public static void start() {
        int port = 8080;
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(port);

        app.post("/api/analyze", AeApiServer::handleAnalyze);
        app.get("/health", ctx -> ctx.result("OK"));

        System.out.println("Afferent/Efferent API server started on port " + port);
    }

    private static void handleAnalyze(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String repoUrl = body.get("github_link");

        if (repoUrl == null || repoUrl.isBlank()) {
            ctx.status(400).result("Missing github_link in request body");
            return;
        }

        try {
            System.out.println("Analyzing repository: " + repoUrl);
            
            // Step 1: Clone the repository
            String localPath = CloneObject.cloneRepository(repoUrl);
            
            // Step 2: Analyze the repository
            List<Metrics> results = Analyzer.analyze(localPath);
            
            // Step 3: Write results to database
            MetricDbWriter.writeAfferentEfferent(results);
            
            // Step 4: Return results
            ctx.json(results);
            System.out.println("Analysis complete for: " + repoUrl);
            
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Analysis failed: " + e.getMessage());
        }
    }
}
