package edu.asu.ser516.taiga;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public final class TaigaApiServer {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin.create()
                .get("/health", ctx -> ctx.result("ok"))
                .get("/taiga/stories", TaigaApiServer::handleStories)
                .get("/taiga/sprint", TaigaApiServer::handleSprint)
                .get("/taiga/projects", TaigaApiServer::handleProjects)
                .get("/taiga/project_velocity", TaigaApiServer::handleProjectVelocity)
                .start(8080);
    }

    private static void handleStories(Context ctx) {
        String projectIdParam = ctx.queryParam("project_id");
        String sprintIdParam = ctx.queryParam("sprint_id");

        if (projectIdParam == null || projectIdParam.isBlank()) {
            ctx.status(400);
            ctx.result("{\"error\":\"Missing project_id\"}");
            return;
        }
        if (sprintIdParam == null || sprintIdParam.isBlank()) {
            ctx.status(400);
            ctx.result("{\"error\":\"Missing sprint_id\"}");
            return;
        }

        int projectId, sprintId;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            sprintId = Integer.parseInt(sprintIdParam.trim());
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("{\"error\":\"project_id and sprint_id must be integers\"}");
            return;
        }

        TaigaLoginObject login = TaigaLoginObject.fromEnv();
        TaigaClient taiga = new TaigaClient();

        try {
            if (!taiga.login(login)) {
                ctx.status(401);
                ctx.result("{\"error\":\"Taiga authentication failed\"}");
                return;
            }
            List<Map<String, Object>> stories = taiga.getStoriesForSprint(login, projectId, sprintId);
            ctx.contentType("application/json");
            ctx.result(mapper.writeValueAsString(stories));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private static void handleSprint(Context ctx) {
        String sprintIdParam = ctx.queryParam("sprint_id");

        if (sprintIdParam == null || sprintIdParam.isBlank()) {
            ctx.status(400);
            ctx.result("{\"error\":\"Missing sprint_id\"}");
            return;
        }

        int sprintId;
        try {
            sprintId = Integer.parseInt(sprintIdParam.trim());
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("{\"error\":\"sprint_id must be an integer\"}");
            return;
        }

        TaigaLoginObject login = TaigaLoginObject.fromEnv();
        TaigaClient taiga = new TaigaClient();

        try {
            if (!taiga.login(login)) {
                ctx.status(401);
                ctx.result("{\"error\":\"Taiga authentication failed\"}");
                return;
            }
            String start = taiga.getSprintStartDate(login, sprintId);
            String end = taiga.getSprintEndDate(login, sprintId);
            ctx.contentType("application/json");
            ctx.result(String.format(
                    "{\"sprintId\":%d,\"sprintStart\":\"%s\",\"sprintEnd\":\"%s\"}",
                    sprintId, start, end));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private static void handleProjects(Context ctx) {
        TaigaLoginObject login = TaigaLoginObject.fromEnv();
        TaigaClient taiga = new TaigaClient();

        try {
            if (!taiga.login(login)) {
                ctx.status(401);
                ctx.result("{\"error\":\"Taiga authentication failed\"}");
                return;
            }
            List<Map<String, Object>> projects = taiga.getProjects(login);
            ctx.contentType("application/json");
            ctx.result(mapper.writeValueAsString(projects));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private static void handleProjectVelocity(Context ctx) {
        String projectIdParam = ctx.queryParam("project_id");
        if (projectIdParam == null || projectIdParam.isBlank()) {
            ctx.status(400);
            ctx.result("{\"error\":\"Missing project_id\"}");
            return;
        }

        int projectId;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("{\"error\":\"project_id must be an integer\"}");
            return;
        }

        TaigaLoginObject login = TaigaLoginObject.fromEnv();
        TaigaClient taiga = new TaigaClient();

        try {
            if (!taiga.login(login)) {
                ctx.status(401);
                ctx.result("{\"error\":\"Taiga authentication failed\"}");
                return;
            }
            Map<String, Object> velocity = taiga.fetchProjectVelocity(login, projectId);
            ctx.contentType("application/json");
            ctx.result(mapper.writeValueAsString(velocity));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}