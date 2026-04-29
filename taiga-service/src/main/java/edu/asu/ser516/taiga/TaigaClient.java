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
}