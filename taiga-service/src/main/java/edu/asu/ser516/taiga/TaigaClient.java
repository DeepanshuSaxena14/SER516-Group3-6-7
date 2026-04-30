package edu.asu.ser516.taiga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class TaigaClient {

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaigaClient() {
        String url = System.getenv("TAIGA_BASE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("TAIGA_BASE_URL environment variable is not set.");
        }
        this.baseUrl = url;
        this.http = buildHttpClient();
    }

    /** For tests — allows injecting a mock server URL. */
    public TaigaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = buildHttpClient();
    }

    private static Connection getDbConnection() throws SQLException {
        String jdbcUrl = System.getenv("JDBC_URL");
        String jdbcUser = System.getenv("JDBC_USER");
        String jdbcPassword = System.getenv("JDBC_PASSWORD");
        
        if (jdbcUrl == null || jdbcUser == null || jdbcPassword == null) {
            throw new SQLException("Database credentials not set: JDBC_URL, JDBC_USER, JDBC_PASSWORD required");
        }
        
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    private static void writeProjectVelocityToDb(int projectId, int sprintId, String sprintName,
                                                  Object sprintStart, Object sprintEnd, double velocity)
            throws SQLException {
        String sql = "INSERT INTO public.sprint_velocity (project_id, sprint_id, sprint_name, sprint_start_date, sprint_end_date, velocity) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (project_id, sprint_id) DO UPDATE SET " +
                     "sprint_name = EXCLUDED.sprint_name, sprint_start_date = EXCLUDED.sprint_start_date, " +
                     "sprint_end_date = EXCLUDED.sprint_end_date, velocity = EXCLUDED.velocity, " +
                     "recorded_at = NOW()";
        
        try (Connection conn = getDbConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            stmt.setInt(2, sprintId);
            stmt.setString(3, sprintName);
            stmt.setDate(4, toSqlDate(sprintStart));
            stmt.setDate(5, toSqlDate(sprintEnd));
            stmt.setDouble(6, velocity);
            stmt.executeUpdate();
        }
    }

    /** Converts a Taiga date string ("YYYY-MM-DD") to java.sql.Date; returns null if blank/invalid. */
    private static java.sql.Date toSqlDate(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return java.sql.Date.valueOf(s);
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Could not parse date '" + s + "': " + e.getMessage());
            return null;
        }
    }

    private static HttpClient buildHttpClient() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] c, String a) {
                }

                public void checkServerTrusted(X509Certificate[] c, String a) {
                }
            } }, new java.security.SecureRandom());
            return HttpClient.newBuilder().sslContext(sc).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTP client", e);
        }
    }

    public boolean login(TaigaLoginObject loginObj) throws Exception {
        String body = "{\"username\":\"" + loginObj.getUsername()
                + "\",\"password\":\"" + loginObj.getPassword()
                + "\",\"type\":\"normal\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = mapper.readValue(response.body(), Map.class);
            loginObj.setAuthToken((String) json.get("auth_token"));
            Object idObj = json.get("id");
            if (idObj instanceof Number) {
                loginObj.setUserId(((Number) idObj).intValue());
            }
            return true;
        }
        return false;
    }

    public String getSprintStartDate(TaigaLoginObject loginObj, int sprintId) throws Exception {
        Map<String, Object> sprint = fetchMilestone(loginObj, sprintId);
        if (sprint == null)
            return null;
        Object val = sprint.get("estimated_start");
        return val != null ? val.toString() : null;
    }

    public String getSprintEndDate(TaigaLoginObject loginObj, int sprintId) throws Exception {
        Map<String, Object> sprint = fetchMilestone(loginObj, sprintId);
        if (sprint == null)
            return null;
        Object val = sprint.get("estimated_finish");
        return val != null ? val.toString() : null;
    }

    public List<Map<String, Object>> getStoriesForSprint(
            TaigaLoginObject loginObj, int projectId, int sprintId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl
                        + "/userstories?project=" + projectId
                        + "&milestone=" + sprintId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve stories: HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
        });
    }

    // sprints are called milestones in the taiga API
    public List<Map<String, Object>> getSprints(TaigaLoginObject loginObj, int projectId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/milestones?project=" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve sprints: HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> getProjects(TaigaLoginObject loginObj) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/projects?member=" + loginObj.getUserId()))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve projects: HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchMilestone(TaigaLoginObject loginObj, int sprintId)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/milestones/" + sprintId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404)
            return null;
        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve sprint " + sprintId + ": HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), Map.class);
    }

    // Fetch project velocity
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchProjectVelocity(TaigaLoginObject loginObj, int projectId)
            throws Exception {
        
        // get all milestones for the project
        HttpRequest milestonesReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/milestones?project=" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> milestonesRes = http.send(milestonesReq, HttpResponse.BodyHandlers.ofString());
        if (milestonesRes.statusCode() != 200) {
            throw new Exception("Failed to fetch milestones: " + milestonesRes.statusCode());
        }

        List<Map<String, Object>> milestones = mapper.readValue(
                milestonesRes.body(),
                new TypeReference<List<Map<String, Object>>>() {});

        // velocity map: sprint_id -> velocity
        Map<String, Object> velocities = new java.util.LinkedHashMap<>();
        
        for (Map<String, Object> sprint : milestones) {
            int sprintId = ((Number) sprint.get("id")).intValue();
            String sprintName = (String) sprint.get("name");
            Object sprintStart = sprint.get("estimated_start");
            Object sprintEnd = sprint.get("estimated_finish");

            // get user stories for this sprint
            HttpRequest storiesReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/userstories?project=" + projectId + "&milestone=" + sprintId))
                    .header("Authorization", "Bearer " + loginObj.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> storiesRes = http.send(storiesReq, HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> stories = new java.util.ArrayList<>();
            
            if (storiesRes.statusCode() == 200) {
                stories = mapper.readValue(
                        storiesRes.body(),
                        new TypeReference<List<Map<String, Object>>>() {});
            }

            // calculate velocity
            double velocity = 0;
            for (Map<String, Object> story : stories) {
                Object isClosed = story.get("is_closed");
                if (isClosed instanceof Boolean && (Boolean) isClosed) {
                    Object totalPoints = story.get("total_points");
                    Object estimatedPoints = story.get("estimated_points");
                    
                    double points = 0;
                    if (totalPoints instanceof Number) {
                        points = ((Number) totalPoints).doubleValue();
                    } else if (estimatedPoints instanceof Number) {
                        points = ((Number) estimatedPoints).doubleValue();
                    }
                    velocity += points;
                }
            }

            // store sprint velocity data
            Map<String, Object> sprintVelocity = new java.util.LinkedHashMap<>();
            sprintVelocity.put("sprintId", sprintId);
            sprintVelocity.put("sprintName", sprintName);
            sprintVelocity.put("sprintStart", sprintStart);
            sprintVelocity.put("sprintEnd", sprintEnd);
            sprintVelocity.put("velocity", velocity);  // double, preserves fractional points
            
            velocities.put(String.valueOf(sprintId), sprintVelocity);
            
            // write to database
            try {
                writeProjectVelocityToDb(projectId, sprintId, sprintName, sprintStart, sprintEnd, velocity);
            } catch (SQLException e) {
                System.err.println("Warning: Failed to write velocity to database: " + e.getMessage());
            }
        }

        return velocities;
    }
}