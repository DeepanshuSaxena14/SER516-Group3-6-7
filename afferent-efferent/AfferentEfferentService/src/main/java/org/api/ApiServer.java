package org.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.github.CloneObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.service.Analyzer;
import org.service.Metrics;
import org.taiga.CruftMetrics;
import org.taiga.DeliveryMetrics;
import org.taiga.TaigaClient;
import org.taiga.TaigaLoginObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApiServer {

    static Map<String, TaigaLoginObject> sessions = new HashMap<>();

    static TaigaClient taigaClient = new TaigaClient();

    public static void start(int port) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/projects", new ProjectsHandler());
        server.createContext("/api/metrics/delivery", new DeliveryHandler());
        server.createContext("/api/metrics/cruft", new CruftHandler());
        server.createContext("/api/analyze", new AnalyzeHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("server started on port " + port);
    }

    static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            try {
                String body = readBody(exchange);
                System.out.println("login attempt: " + body);

                JSONObject req = new JSONObject(body);
                String username = req.getString("username");
                String password = req.getString("password");

                TaigaLoginObject loginObj = new TaigaLoginObject(username, password);
                boolean ok = taigaClient.login(loginObj);

                if (!ok) {
                    System.out.println("login failed for user: " + username);
                    sendResponse(exchange, 401, "{\"error\": \"login failed\"}");
                    return;
                }

                String token = UUID.randomUUID().toString();
                sessions.put(token, loginObj);
                System.out.println("user logged in: " + username + " token: " + token);

                JSONObject resp = new JSONObject();
                resp.put("token", token);
                resp.put("userId", loginObj.getUserId());
                sendResponse(exchange, 200, resp.toString());

            } catch (Exception e) {
                System.out.println("error in login: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\": \"something went wrong\"}");
            }
        }
    }

    static class ProjectsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String token = params.get("token");

            if (token == null || !sessions.containsKey(token)) {
                sendResponse(exchange, 401, "{\"error\": \"not logged in\"}");
                return;
            }

            try {
                TaigaLoginObject loginObj = sessions.get(token);
                String projectsJson = taigaClient.getProjects(loginObj);
                sendResponse(exchange, 200, projectsJson);
            } catch (Exception e) {
                System.out.println("error getting projects: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class DeliveryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String token = params.get("token");
            String projectIdStr = params.get("projectId");

            if (token == null || !sessions.containsKey(token)) {
                sendResponse(exchange, 401, "{\"error\": \"not logged in\"}");
                return;
            }

            if (projectIdStr == null) {
                sendResponse(exchange, 400, "{\"error\": \"need a projectId\"}");
                return;
            }

            try {
                int projectId = Integer.parseInt(projectIdStr);
                TaigaLoginObject loginObj = sessions.get(token);

                List<DeliveryMetrics> metrics = taigaClient.getDeliveryMetrics(loginObj, projectId);

                JSONArray result = new JSONArray();
                for (DeliveryMetrics m : metrics) {
                    JSONObject obj = new JSONObject();
                    obj.put("sprintName", m.sprintName());
                    obj.put("totalTasks", m.totalTasks());
                    obj.put("onTime", m.onTime());
                    obj.put("late", m.late());
                    result.put(obj);
                }

                sendResponse(exchange, 200, result.toString());

            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\": \"projectId has to be a number\"}");
            } catch (Exception e) {
                System.out.println("delivery metrics error: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class CruftHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String token = params.get("token");
            String projectIdStr = params.get("projectId");

            if (token == null || !sessions.containsKey(token)) {
                sendResponse(exchange, 401, "{\"error\": \"not logged in\"}");
                return;
            }

            if (projectIdStr == null) {
                sendResponse(exchange, 400, "{\"error\": \"need a projectId\"}");
                return;
            }

            try {
                int projectId = Integer.parseInt(projectIdStr);
                TaigaLoginObject loginObj = sessions.get(token);

                List<CruftMetrics> cruftList = taigaClient.getCruftMetrics(loginObj, projectId);

                JSONArray result = new JSONArray();
                for (CruftMetrics c : cruftList) {
                    JSONObject obj = new JSONObject();
                    obj.put("sprintName", c.sprintName());
                    obj.put("startDate", c.startDate());
                    obj.put("endDate", c.endDate());
                    obj.put("totalStories", c.totalStories());
                    obj.put("cruftStories", c.cruftStories());
                    obj.put("cruftPercentage", c.cruftPercentage());
                    result.put(obj);
                }

                sendResponse(exchange, 200, result.toString());

            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\": \"projectId has to be a number\"}");
            } catch (Exception e) {
                System.out.println("cruft metrics error: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            try {
                String body = readBody(exchange);
                JSONObject req = new JSONObject(body);
                String repoUrl = req.getString("repoUrl");

                if (!repoUrl.contains("github.com/")) {
                    sendResponse(exchange, 400, "{\"error\": \"only github urls work right now\"}");
                    return;
                }

                System.out.println("cloning: " + repoUrl);
                CloneObject.getRepoMetadata(repoUrl);
                String localPath = CloneObject.cloneRepository(repoUrl);

                List<Metrics> results = Analyzer.analyze(localPath);

                JSONArray arr = new JSONArray();
                for (Metrics m : results) {
                    JSONObject obj = new JSONObject();
                    obj.put("className", m.getClassName());
                    obj.put("afferent", m.getAfferent());
                    obj.put("efferent", m.getEfferent());
                    arr.put(obj);
                }

                sendResponse(exchange, 200, arr.toString());

            } catch (Exception e) {
                System.out.println("analyze failed: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}
