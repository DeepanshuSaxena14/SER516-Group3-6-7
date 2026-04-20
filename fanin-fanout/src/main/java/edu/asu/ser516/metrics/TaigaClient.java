package edu.asu.ser516.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client for the Taiga REST API (v1) used by
 * {@link MetricsApiServer#handleTaigaAuc}.
 *
 * <p>
 * Only the subset of endpoints required by the AUC handler is implemented
 * here; everything else lives in the separate TaigaService module.
 */
public final class TaigaClient {

    private final String baseUrl;

    public TaigaClient() {
        String url = System.getenv("TAIGA_BASE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("TAIGA_BASE_URL environment variable is not set.");
        }
        this.baseUrl = url;
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    /**
     * Logs in to Taiga and populates {@code loginObj} with the auth token and
     * user ID on success.
     *
     * @return {@code true} if the login was accepted, {@code false} if Taiga
     *         returned a non-200 status (e.g. bad credentials).
     * @throws Exception on network or I/O errors
     */
    public boolean login(TaigaLoginObject loginObj) throws Exception {
        String body = "{\"username\":\"" + loginObj.getUsername()
                + "\",\"password\":\"" + loginObj.getPassword()
                + "\",\"type\":\"normal\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth"))
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

    // -------------------------------------------------------------------------
    // Sprint (milestone) helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code estimated_start} date string (ISO-8601, e.g.
     * {@code "2024-01-15"}) for the given sprint (milestone), or {@code null}
     * if the field is absent or the sprint cannot be found.
     */
    public String getSprintStartDate(TaigaLoginObject loginObj, int sprintId) throws Exception {
        Map<String, Object> sprint = fetchMilestone(loginObj, sprintId);
        if (sprint == null)
            return null;
        Object val = sprint.get("estimated_start");
        return val != null ? val.toString() : null;
    }

    /**
     * Returns the {@code estimated_finish} date string (ISO-8601, e.g.
     * {@code "2024-01-29"}) for the given sprint (milestone), or {@code null}
     * if the field is absent or the sprint cannot be found.
     */
    public String getSprintEndDate(TaigaLoginObject loginObj, int sprintId) throws Exception {
        Map<String, Object> sprint = fetchMilestone(loginObj, sprintId);
        if (sprint == null)
            return null;
        Object val = sprint.get("estimated_finish");
        return val != null ? val.toString() : null;
    }

    // -------------------------------------------------------------------------
    // User stories
    // -------------------------------------------------------------------------

    /**
     * Returns the list of user stories assigned to {@code sprintId} inside
     * {@code projectId}. Each story is a raw {@code Map<String, Object>}
     * matching the Taiga API response fields (e.g. {@code subject},
     * {@code total_points}, {@code finish_date}, {@code is_closed}, etc.).
     *
     * @return a non-null, possibly empty list
     */
    public List<Map<String, Object>> getStoriesForSprint(
            TaigaLoginObject loginObj, int projectId, int sprintId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL
                        + "/userstories?project=" + projectId
                        + "&milestone=" + sprintId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve stories for sprint " + sprintId
                    + ": HTTP " + response.statusCode());
        }

        return mapper.readValue(
                response.body(),
                new TypeReference<List<Map<String, Object>>>() {
                });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Fetches a single milestone by its ID. Returns {@code null} on 404. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchMilestone(TaigaLoginObject loginObj, int sprintId)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/milestones/" + sprintId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404)
            return null;
        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve sprint " + sprintId
                    + ": HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), Map.class);
    }
}
