package edu.asu.ser516.metrics;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaigaClientTest {

    // helper to build a mock taiga server so we dont hit the real api in tests
    private Javalin mockTaigaServer() {
        return Javalin.create()
            .post("/auth", ctx -> {
                String body = ctx.body();
                if (body.contains("badpass")) {
                    ctx.status(400).result("{\"error\":\"login failed\"}");
                } else {
                    ctx.status(200).result("{\"auth_token\":\"test-token\",\"id\":42}");
                }
            })
            .get("/milestones", ctx -> {
                ctx.status(200).result("[{\"id\":1,\"name\":\"Sprint 1\",\"estimated_start\":\"2024-01-01\",\"estimated_finish\":\"2024-01-14\"}]");
            })
            .get("/milestones/{id}", ctx -> {
                int id = Integer.parseInt(ctx.pathParam("id"));
                if (id == 999) {
                    ctx.status(404).result("{}");
                } else {
                    ctx.status(200).result("{\"id\":" + id + ",\"estimated_start\":\"2024-01-01\",\"estimated_finish\":\"2024-01-14\"}");
                }
            })
            .get("/userstories", ctx -> {
                ctx.status(200).result("[{\"id\":1,\"subject\":\"Story 1\",\"total_points\":5,\"business_value\":3,\"finish_date\":\"2024-01-10\",\"is_closed\":true}]");
            });
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns true and sets auth token on success")
        void loginSuccess() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "pass");

                boolean result = taiga.login(login);

                assertTrue(result);
                assertEquals("test-token", login.getAuthToken());
                assertEquals(42, login.getUserId());
            });
        }

        @Test
        @DisplayName("returns false when taiga rejects credentials")
        void loginFailure() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "badpass");

                boolean result = taiga.login(login);

                assertFalse(result);
            });
        }
    }

    @Nested
    @DisplayName("getSprints()")
    class GetSprints {

        @Test
        @DisplayName("returns a list of sprints for a project")
        void returnsSprints() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "pass");
                taiga.login(login);

                List<Map<String, Object>> sprints = taiga.getSprints(login, 1);

                assertNotNull(sprints);
                assertEquals(1, sprints.size());
                assertEquals("Sprint 1", sprints.get(0).get("name"));
            });
        }
    }

    @Nested
    @DisplayName("getStoriesForSprint()")
    class GetStories {

        @Test
        @DisplayName("returns stories with story points, business value, and completion date")
        void returnsStoryData() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "pass");
                taiga.login(login);

                List<Map<String, Object>> stories = taiga.getStoriesForSprint(login, 1, 1);

                assertNotNull(stories);
                assertEquals(1, stories.size());
                Map<String, Object> story = stories.get(0);
                assertEquals(5, story.get("total_points"));
                assertEquals(3, story.get("business_value"));
                assertEquals("2024-01-10", story.get("finish_date"));
                assertTrue((Boolean) story.get("is_closed"));
            });
        }
    }

    @Nested
    @DisplayName("getSprintStartDate() and getSprintEndDate()")
    class SprintDates {

        @Test
        @DisplayName("returns start date for a valid sprint id")
        void returnsStartDate() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "pass");
                taiga.login(login);

                String startDate = taiga.getSprintStartDate(login, 1);

                assertEquals("2024-01-01", startDate);
            });
        }

        @Test
        @DisplayName("returns end date for a valid sprint id")
        void returnsEndDate() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "pass");
                taiga.login(login);

                String endDate = taiga.getSprintEndDate(login, 1);

                assertEquals("2024-01-14", endDate);
            });
        }

        @Test
        @DisplayName("returns null for a sprint id that doesnt exist")
        void returnsNullFor404() {
            JavalinTest.test(mockTaigaServer(), (server, client) -> {
                TaigaClient taiga = new TaigaClient("http://localhost:" + server.port());
                TaigaLoginObject login = new TaigaLoginObject("user", "pass");
                taiga.login(login);

                String startDate = taiga.getSprintStartDate(login, 999);

                assertNull(startDate);
            });
        }
    }
}
