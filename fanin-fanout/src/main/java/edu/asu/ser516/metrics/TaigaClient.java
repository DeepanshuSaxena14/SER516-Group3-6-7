package edu.asu.ser516.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

// adapted from the AE service TaigaClient - reused here so we dont duplicate code
public class TaigaClient {

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaigaClient() {
        String url = System.getenv("TAIGA_BASE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("TAIGA_BASE_URL environment variable is not set");
        }
        this.baseUrl = url;
        this.http = buildHttpClient();
    }

    // for tests - lets us inject a mock server url
    public TaigaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = buildHttpClient();
    }

    // skip ssl verification - needed for some environments
    private HttpClient buildHttpClient() {
        try {
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
            sc.init(null, new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                }
            }, new java.security.SecureRandom());
            return HttpClient.newBuilder().sslContext(sc).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTP client", e);
        }
    }

    // logs in and stores the auth token + user id in the login object
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
            Map<String, Object> json = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            loginObj.setAuthToken((String) json.get("auth_token"));
            Object idObj = json.get("id");
            if (idObj instanceof Number) {
                loginObj.setUserId(((Number) idObj).intValue());
            }
            return true;
        }

        System.out.println("Login failed. Status: " + response.statusCode());
        return false;
    }

    // sprints are called milestones in the taiga API for some reason
    public List<Map<String, Object>> getSprints(TaigaLoginObject loginObj, int projectId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/milestones?project=" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get sprints: " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
    }

    // returns user stories for a sprint - includes story points, business value, and completion dates
    public List<Map<String, Object>> getStoriesForSprint(
            TaigaLoginObject loginObj, int projectId, int sprintId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/userstories?project=" + projectId + "&milestone=" + sprintId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get stories for sprint " + sprintId + ": " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
    }

    // get start date for a sprint by id
    public String getSprintStartDate(TaigaLoginObject loginObj, int sprintId) throws Exception {
        Map<String, Object> sprint = fetchMilestone(loginObj, sprintId);
        if (sprint == null) return null;
        Object val = sprint.get("estimated_start");
        return val != null ? val.toString() : null;
    }

    // get end date for a sprint by id
    public String getSprintEndDate(TaigaLoginObject loginObj, int sprintId) throws Exception {
        Map<String, Object> sprint = fetchMilestone(loginObj, sprintId);
        if (sprint == null) return null;
        Object val = sprint.get("estimated_finish");
        return val != null ? val.toString() : null;
    }

    private Map<String, Object> fetchMilestone(TaigaLoginObject loginObj, int sprintId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/milestones/" + sprintId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return null;
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get sprint " + sprintId + ": " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
    }
}
