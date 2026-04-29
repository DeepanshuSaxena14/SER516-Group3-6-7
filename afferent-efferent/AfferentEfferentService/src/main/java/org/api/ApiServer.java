package org.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.service.Analyzer;
import org.service.Metrics;
import org.github.CloneObject;
import org.taiga.CruftMetrics;
import org.taiga.DeliveryMetrics;
import org.taiga.FocusFactorMetrics;
import org.taiga.TaigaClient;
import org.taiga.TaigaLoginObject;

import java.io.IOException;
import java.io.InputStream;
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
        server.createContext("/api/metrics/focus-factor", new FocusFactorHandler());
        server.createContext("/api/analyze", new AnalyzeHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("server started on port " + port);
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("login attempt: " + body);

                JSONObject req = new JSONObject(body);
                String username = req.getString("username");
                String password = req.getString("password");

                TaigaLoginObject loginObj = new TaigaLoginObject(username, password);
                boolean ok = taigaClient.login(loginObj);

                if (!ok) {
                    String resp = "{\"error\": \"login failed\"}";
                    exchange.sendResponseHeaders(401, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                String token = UUID.randomUUID().toString();
                sessions.put(token, loginObj);
                System.out.println("logged in: " + username + " token: " + token);

                String resp = "{\"token\": \"" + token + "\"}";
                exchange.sendResponseHeaders(200, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            } catch (Exception e) {
                System.out.println("error in login: " + e.getMessage());
                String resp = "{\"error\": \"something went wrong\"}";
                exchange.sendResponseHeaders(500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }

    static class ProjectsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = new HashMap<>();
                if (query != null) {
                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) params.put(kv[0], kv[1]);
                    }
                }

                String token = params.get("token");
                TaigaLoginObject loginObj = sessions.get(token);
                if (loginObj == null) {
                    String resp = "{\"error\": \"invalid token\"}";
                    exchange.sendResponseHeaders(401, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                String projects = taigaClient.getProjects(loginObj);
                byte[] bytes = projects.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                String resp = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }

    static class DeliveryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = new HashMap<>();
                if (query != null) {
                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) params.put(kv[0], kv[1]);
                    }
                }

                String token = params.get("token");
                TaigaLoginObject loginObj = sessions.get(token);
                if (loginObj == null) {
                    String resp = "{\"error\": \"invalid token\"}";
                    exchange.sendResponseHeaders(401, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                int projectId = Integer.parseInt(params.get("projectId"));
                List<DeliveryMetrics> metrics = taigaClient.getDeliveryMetrics(loginObj, projectId);

                JSONArray arr = new JSONArray();
                for (DeliveryMetrics m : metrics) {
                    JSONObject obj = new JSONObject();
                    obj.put("sprint", m.sprintName());
                    obj.put("total", m.totalTasks());
                    obj.put("onTime", m.onTime());
                    obj.put("late", m.late());
                    arr.put(obj);
                }

                byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                String resp = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }

    static class CruftHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = new HashMap<>();
                if (query != null) {
                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) params.put(kv[0], kv[1]);
                    }
                }

                String token = params.get("token");
                TaigaLoginObject loginObj = sessions.get(token);
                if (loginObj == null) {
                    String resp = "{\"error\": \"invalid token\"}";
                    exchange.sendResponseHeaders(401, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                int projectId = Integer.parseInt(params.get("projectId"));
                List<CruftMetrics> metrics = taigaClient.getCruftMetrics(loginObj, projectId);

                JSONArray arr = new JSONArray();
                for (CruftMetrics m : metrics) {
                    JSONObject obj = new JSONObject();
                    obj.put("sprint", m.sprintName());
                    obj.put("startDate", m.startDate());
                    obj.put("endDate", m.endDate());
                    obj.put("totalStories", m.totalStories());
                    obj.put("cruftStories", m.cruftStories());
                    obj.put("cruftPercent", m.cruftPercentage());
                    arr.put(obj);
                }

                byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                String resp = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }

    static class FocusFactorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = new HashMap<>();
                if (query != null) {
                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) params.put(kv[0], kv[1]);
                    }
                }

                String token = params.get("token");
                TaigaLoginObject loginObj = sessions.get(token);
                if (loginObj == null) {
                    String resp = "{\"error\": \"invalid token\"}";
                    exchange.sendResponseHeaders(401, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                int projectId = Integer.parseInt(params.get("projectId"));
                List<FocusFactorMetrics> metrics = taigaClient.getFocusFactorMetrics(loginObj, projectId);

                JSONArray arr = new JSONArray();
                for (FocusFactorMetrics m : metrics) {
                    JSONObject obj = new JSONObject();
                    obj.put("sprint", m.sprintName());
                    obj.put("workCapacity", m.workCapacity());
                    obj.put("velocity", m.velocity());
                    obj.put("focusFactor", m.focusFactor());
                    arr.put(obj);
                }

                byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                String resp = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }

    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject req = new JSONObject(body);
                String githubLink = req.getString("github_link");

                String localPath = CloneObject.cloneRepository(githubLink);
                List<Metrics> results = Analyzer.analyze(localPath);

                JSONArray arr = new JSONArray();
                for (Metrics m : results) {
                    JSONObject obj = new JSONObject();
                    obj.put("className", m.getClassName());
                    obj.put("afferent", m.getAfferent());
                    obj.put("efferent", m.getEfferent());
                    arr.put(obj);
                }

                byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                String resp = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }
}
