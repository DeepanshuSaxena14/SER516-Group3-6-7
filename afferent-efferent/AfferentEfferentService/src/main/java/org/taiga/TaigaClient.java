package org.taiga;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// this class handles all the calls to the taiga API
public class TaigaClient {

    private static final String BASE_URL = "https://swent0linux.asu.edu/taiga/api/v1";

    private final HttpClient client = buildClient();

    private static HttpClient buildClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Exception e) {
            return HttpClient.newHttpClient();
        }
    }

    // logs in and stores the auth token + user id in the login object
    public boolean login(TaigaLoginObject loginObj) throws Exception {

        // taiga wants the body as json, had to look up the format
        String body = "{\"username\": \"" + loginObj.getUsername() +
                "\", \"password\": \"" + loginObj.getPassword() +
                "\", \"type\": \"normal\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // pull the token and user id out of the response so we can use them later
            loginObj.setAuthToken(extractString(response.body(), "auth_token"));
            loginObj.setUserId(extractInt(response.body(), "id"));
            return true;
        }

        System.out.println("Login failed. Status: " + response.statusCode());
        return false;
    }

    // returns the raw projects list, use listProjects() if you want it parsed
    public String getProjects(TaigaLoginObject loginObj) throws Exception {
        // filter by member so we only get projects the user is part of
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/projects?member=" + loginObj.getUserId()))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get projects: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    // prints a readable list of the users projects with id, name, and slug
    public void listProjects(TaigaLoginObject loginObj) throws Exception {
        String body = getProjects(loginObj);
        JSONArray projects = new JSONArray(body);

        System.out.println("Your projects:");
        for (int i = 0; i < projects.length(); i++) {
            JSONObject p = projects.getJSONObject(i);
            System.out.println("  ID: " + p.getInt("id") +
                    " | Name: " + p.getString("name") +
                    " | Slug: " + p.getString("slug"));
        }
    }

    public String getProjectById(TaigaLoginObject loginObj, int projectId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/projects/" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get project: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    // look up a project by its slug instead of its id
    public String getProjectBySlug(TaigaLoginObject loginObj, String slug) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/projects/by_slug?slug=" + slug))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("Could not find project with slug: " + slug + " (status " + response.statusCode() + ")");
            return null;
        }

        return response.body();
    }

    public String getUserStories(TaigaLoginObject loginObj, int projectId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/userstories?project=" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get user stories: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    public String getTasks(TaigaLoginObject loginObj, int projectId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks?project=" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get tasks: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    // sprints are called milestones in the taiga API for some reason
    public String getSprints(TaigaLoginObject loginObj, int projectId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/milestones?project=" + projectId))
                .header("Authorization", "Bearer " + loginObj.getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get sprints: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    public List<DeliveryMetrics> getDeliveryMetrics(TaigaLoginObject loginObj, int projectId) throws Exception {
        List<DeliveryMetrics> metrics = new ArrayList<>();
        String sprintsJson = getSprints(loginObj, projectId);
        JSONArray sprints = new JSONArray(sprintsJson);
        for (int i = 0; i < sprints.length(); i++) {
            JSONObject sprint = sprints.getJSONObject(i);
            String name = sprint.getString("name");
            if (!sprint.has("estimated_finish") || sprint.isNull("estimated_finish"))
                continue;
            String endDateStr = sprint.getString("estimated_finish");
            LocalDate endDate;
            try {
                endDate = LocalDate.parse(endDateStr.substring(0, 10));
            } catch (Exception e) {
                continue;
            }
            int sprintId = sprint.getInt("id");

            // get tasks for this sprint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/tasks?project=" + projectId + "&milestone=" + sprintId))
                    .header("Authorization", "Bearer " + loginObj.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // skip this sprint if can't get tasks
                continue;
            }
            String tasksJson = response.body();
            JSONArray tasks = new JSONArray(tasksJson);

            int total = 0;
            int onTime = 0;
            for (int j = 0; j < tasks.length(); j++) {
                JSONObject task = tasks.getJSONObject(j);
                total++;
                if (task.has("status_extra_info")) {
                    JSONObject statusInfo = task.getJSONObject("status_extra_info");
                    String status = statusInfo.getString("name");
                    if ("Closed".equals(status) || "Done".equals(status)) {
                        if (task.has("finished_date") && !task.isNull("finished_date")) {
                            String finishDateStr = task.getString("finished_date");
                            try {
                                LocalDate finishDate = LocalDate.parse(finishDateStr.substring(0, 10));
                                if (!finishDate.isAfter(endDate)) {
                                    onTime++;
                                }
                            } catch (Exception e) {
                                // consider late
                            }
                        }
                    }
                }
            }
            int late = total - onTime;
            metrics.add(new DeliveryMetrics(name, total, onTime, late));
        }
        return metrics;
    }

    // gets all the data for a project and structures it so we can see it over time
    // basically a java version of the python get_structure function
    public TaigaProject getStructure(TaigaLoginObject loginObj, int projectId) throws Exception {

        // grab everything we need from the API
        String projectBody = getProjectById(loginObj, projectId);
        String sprintsBody = getSprints(loginObj, projectId);
        String userStoriesBody = getUserStories(loginObj, projectId);
        String tasksBody = getTasks(loginObj, projectId);

        JSONObject projectJson = new JSONObject(projectBody);
        JSONArray sprintsJson = new JSONArray(sprintsBody);
        JSONArray userStoriesJson = new JSONArray(userStoriesBody);
        JSONArray tasksJson = new JSONArray(tasksBody);

        // group tasks by user story id
        Map<Integer, List<TaigaProject.Task>> tasksByStory = new HashMap<>();
        for (int i = 0; i < tasksJson.length(); i++) {
            JSONObject taskJson = tasksJson.getJSONObject(i);

            // user_story can be null if the task isn't assigned to a story
            if (taskJson.isNull("user_story"))
                continue;

            int usId = taskJson.getInt("user_story");
            if (!tasksByStory.containsKey(usId)) {
                tasksByStory.put(usId, new ArrayList<>());
            }

            TaigaProject.Task task = new TaigaProject.Task();
            task.id = taskJson.getInt("id");
            task.name = taskJson.getString("subject");
            task.createdDate = taskJson.getString("created_date");
            tasksByStory.get(usId).add(task);
        }

        // group user stories by sprint id
        // milestone in taiga = sprint, milestone can also be null for backlog stories
        Map<Integer, List<TaigaProject.UserStory>> storiesBySprint = new HashMap<>();
        for (int i = 0; i < userStoriesJson.length(); i++) {
            JSONObject usJson = userStoriesJson.getJSONObject(i);

            if (usJson.isNull("milestone"))
                continue;

            int sprintId = usJson.getInt("milestone");
            if (!storiesBySprint.containsKey(sprintId)) {
                storiesBySprint.put(sprintId, new ArrayList<>());
            }

            TaigaProject.UserStory us = new TaigaProject.UserStory();
            us.id = usJson.getInt("id");
            us.name = usJson.getString("subject");
            us.createdDate = usJson.getString("created_date");
            us.tasks = tasksByStory.getOrDefault(us.id, new ArrayList<>());
            storiesBySprint.get(sprintId).add(us);
        }

        // build the final project object
        TaigaProject project = new TaigaProject();
        project.projectName = projectJson.getString("name");
        project.projectId = projectJson.getInt("id");
        project.createdDate = projectJson.getString("created_date");

        // put sprints in, each sprint has its user stories and tasks inside
        for (int i = 0; i < sprintsJson.length(); i++) {
            JSONObject sprintJson = sprintsJson.getJSONObject(i);

            TaigaProject.Sprint sprint = new TaigaProject.Sprint();
            sprint.id = sprintJson.getInt("id");
            sprint.name = sprintJson.getString("name");
            sprint.startDate = sprintJson.optString("estimated_start", "N/A");
            sprint.finishDate = sprintJson.optString("estimated_finish", "N/A");
            sprint.userStories = storiesBySprint.getOrDefault(sprint.id, new ArrayList<>());
            project.sprints.add(sprint);
        }

        return project;
    }

    public List<FocusFactorMetrics> getFocusFactorMetrics(TaigaLoginObject loginObj, int projectId) throws Exception {
        List<FocusFactorMetrics> results = new ArrayList<>();
        String sprintsJson = getSprints(loginObj, projectId);
        JSONArray sprints = new JSONArray(sprintsJson);

        for (int i = 0; i < sprints.length(); i++) {
            JSONObject sprint = sprints.getJSONObject(i);
            int sprintId = sprint.getInt("id");
            String sprintName = sprint.getString("name");

            // fetch user stories assigned to sprint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/userstories?project=" + projectId + "&milestone=" + sprintId))
                    .header("Authorization", "Bearer " + loginObj.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                continue;
            }

            JSONArray stories = new JSONArray(response.body());
            double workCapacity = 0;
            double velocity = 0;

            for (int j = 0; j < stories.length(); j++) {
                JSONObject story = stories.getJSONObject(j);
                double points = story.optDouble("total_points", 0.0);
                if (points < 0) points = 0.0;
                
                workCapacity += points;

                if (story.has("is_closed") && story.getBoolean("is_closed")) {
                    velocity += points;
                }
            }

            double focusFactor = workCapacity == 0 ? 0.0 : (velocity / workCapacity) * 100.0;
            results.add(new FocusFactorMetrics(sprintName, workCapacity, velocity, focusFactor));
        }

        return results;
    }

    // returns cruft ratio for each sprint in the project
    public List<CruftMetrics> getCruftMetrics(TaigaLoginObject loginObj, int projectId) throws Exception {
        List<CruftMetrics> results = new ArrayList<>();
        String sprintsJson = getSprints(loginObj, projectId);
        JSONArray sprints = new JSONArray(sprintsJson);

        for (int i = 0; i < sprints.length(); i++) {
            JSONObject sprint = sprints.getJSONObject(i);
            int sprintId = sprint.getInt("id");
            String sprintName = sprint.getString("name");
            String startDate = sprint.optString("estimated_start", "N/A");
            String endDate = sprint.optString("estimated_finish", "N/A");

            // fetch user stories assigned to sprint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/userstories?project=" + projectId + "&milestone=" + sprintId))
                    .header("Authorization", "Bearer " + loginObj.getAuthToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // skip sprint if stories can't be retrieved
                continue;
            }

            JSONArray stories = new JSONArray(response.body());
            int total = stories.length();
            int cruftCount = 0;

            for (int j = 0; j < stories.length(); j++) {
                if (isCruft(stories.getJSONObject(j))) {
                    cruftCount++;
                }
            }

            double cruftPct = total == 0 ? 0.0 : (cruftCount * 100.0 / total);
            results.add(new CruftMetrics(sprintName, startDate, endDate, total, cruftCount, cruftPct));
        }

        return results;
    }

    private boolean isCruft(JSONObject story) {
        Set<String> CRUFT_TAGS = Set.of(
                "bug", "tech-debt", "technical-debt", "tech_debt", "technical_debt",
                "refactor", "refactoring", "infrastructure", "infra",
                "chore", "maintenance", "cleanup", "debt", "fix");

        // tag-based detection
        if (story.has("tags") && !story.isNull("tags")) {
            JSONArray tags = story.getJSONArray("tags");
            for (int i = 0; i < tags.length(); i++) {
                Object tagEntry = tags.get(i);
                String tagName = "";

                if (tagEntry instanceof JSONArray) {
                    tagName = ((JSONArray) tagEntry).optString(0, "").toLowerCase().trim();
                } else if (tagEntry instanceof String) {
                    tagName = ((String) tagEntry).toLowerCase().trim();
                }

                if (CRUFT_TAGS.contains(tagName)) {
                    return true;
                }
            }
        }

        // point-based detection
        if (!story.has("total_points") || story.isNull("total_points")) {
            return true;
        }

        double points = story.optDouble("total_points", -1);
        if (points <= 0.0) {
            return true;
        }

        // optional subject-based detection
        String subject = story.optString("subject", "").toLowerCase();
        for (String keyword : CRUFT_TAGS) {
            if (subject.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        while (start < json.length() && json.charAt(start) == ' ')
            start++;
        if (start < json.length() && json.charAt(start) == '"')
            start++;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1)
            end = json.indexOf("}", start);
        return Integer.parseInt(json.substring(start, end).trim());
    }
}
