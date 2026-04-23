package edu.asu.ser516.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class TaigaClient {

    private static final String BASE_URL = "https://swent0linux.asu.edu/taiga/api/v1";

    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaigaClient() {
        HttpClient c;
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);
            c = HttpClient.newBuilder().sslContext(ctx).build();
        } catch (Exception e) {
            c = HttpClient.newHttpClient();
        }
        this.client = c;
    }

    public String login(String username, String password) throws Exception {
        String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\", \"type\": \"normal\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200)
            throw new Exception("login failed: " + res.statusCode());

        return mapper.readTree(res.body()).get("auth_token").asText();
    }

    public List<SprintUserStory> getUserStoriesForSprint(String token, int projectId, int sprintId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/userstories?project=" + projectId + "&milestone=" + sprintId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200)
            throw new Exception("failed to get user stories: " + res.statusCode());

        JsonNode stories = mapper.readTree(res.body());
        List<SprintUserStory> result = new ArrayList<>();

        for (JsonNode story : stories) {
            double totalPoints = story.path("total_points").asDouble(0);
            int businessValue = story.path("business_value").asInt(0);
            String finishDate = story.path("finish_date").isNull() ? null : story.path("finish_date").asText(null);
            result.add(new SprintUserStory(totalPoints, businessValue, finishDate));
        }

        return result;
    }
}
