package edu.asu.ser516.metrics;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaigaClientTest {

    private HttpServer mockServer;
    private TaigaClient client;
    private TaigaLoginObject loginObj;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();
        client = new TaigaClient("http://localhost:" + port);
        loginObj = new TaigaLoginObject("testuser", "testpass");
        mockServer.start();
    }
    @AfterEach
    void tearDown() {
        mockServer.stop(0);
    }

    private void mockEndpoint(String path, int statusCode, String responseBody) {
        mockServer.createContext(path, exchange -> {
            byte[] bytes = responseBody.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void testLoginSuccess() throws Exception {
        mockEndpoint("/api/v1/auth", 200, """
                {
                    "auth_token": "abc123",
                    "id": 42
                }
                """);

        boolean result = client.login(loginObj);

        assertTrue(result, "Login should return true on HTTP 200");
        assertEquals("abc123", loginObj.getAuthToken());
        assertEquals(42, loginObj.getUserId());
    }

    @Test
    void testLoginFailureBadCredentials() throws Exception {
        mockEndpoint("/api/v1/auth", 400, """
                {"detail": "No active account found with the given credentials",
                 "code": "invalid_credentials"}
                """);

        boolean result = client.login(loginObj);

        assertFalse(result, "Login should return false on HTTP 400");
        assertNull(loginObj.getAuthToken(),
                "Auth token should remain null after failed login");
    }

    @Test
    void testLoginFailureServerError() throws Exception {
        mockEndpoint("/api/v1/auth", 500, "Internal Server Error");

        boolean result = client.login(loginObj);

        assertFalse(result, "Login should return false on HTTP 500");
    }

    @Test
    void testGetSprintStartDate() throws Exception {
        mockEndpoint("/api/v1/milestones/10", 200, """
                {
                    "id": 10,
                    "estimated_start": "2024-01-15",
                    "estimated_finish": "2024-01-29"
                }
                """);

        loginObj.setAuthToken("abc123");
        String startDate = client.getSprintStartDate(loginObj, 10);

        assertEquals("2024-01-15", startDate);
    }

    @Test
    void testGetSprintEndDate() throws Exception {
        mockEndpoint("/api/v1/milestones/10", 200, """
                {
                    "id": 10,
                    "estimated_start": "2024-01-15",
                    "estimated_finish": "2024-01-29"
                }
                """);

        loginObj.setAuthToken("abc123");
        String endDate = client.getSprintEndDate(loginObj, 10);

        assertEquals("2024-01-29", endDate);
    }

    @Test
    void testGetSprintStartDateMissingField() throws Exception {
        mockEndpoint("/api/v1/milestones/10", 200, """
                {
                    "id": 10,
                    "estimated_finish": "2024-01-29"
                }
                """);

        loginObj.setAuthToken("abc123");
        String startDate = client.getSprintStartDate(loginObj, 10);

        assertNull(startDate, "Should return null when estimated_start is absent");
    }

    @Test
    void testGetSprintNotFound() throws Exception {
        mockEndpoint("/api/v1/milestones/999", 404, "{}");

        loginObj.setAuthToken("abc123");
        String startDate = client.getSprintStartDate(loginObj, 999);

        assertNull(startDate, "Should return null on 404");
    }

    @Test
    void testGetSprintServerError() throws Exception {
        mockEndpoint("/api/v1/milestones/10", 500, "Internal Server Error");

        loginObj.setAuthToken("abc123");

        assertThrows(Exception.class,
                () -> client.getSprintStartDate(loginObj, 10),
                "Should throw on HTTP 500");
    }
    
    @Test
    void testGetStoriesForSprintHappyPath() throws Exception {
        mockEndpoint("/api/v1/userstories", 200, """
                [
                    {
                        "subject": "Story A",
                        "total_points": 5.0,
                        "is_closed": true,
                        "finish_date": "2024-01-17"
                    },
                    {
                        "subject": "Story B",
                        "total_points": 3.0,
                        "is_closed": false,
                        "finish_date": null
                    }
                ]
                """);

        loginObj.setAuthToken("abc123");
        List<Map<String, Object>> stories =
                client.getStoriesForSprint(loginObj, 1, 10);

        assertEquals(2, stories.size());
        assertEquals(5.0,
                ((Number) stories.get(0).get("total_points")).doubleValue(), 0.01);
        assertEquals(true, stories.get(0).get("is_closed"));
        assertEquals(3.0,
                ((Number) stories.get(1).get("total_points")).doubleValue(), 0.01);
        assertEquals(false, stories.get(1).get("is_closed"));
    }

    @Test
    void testGetStoriesForSprintEmpty() throws Exception {
        mockEndpoint("/api/v1/userstories", 200, "[]");

        loginObj.setAuthToken("abc123");
        List<Map<String, Object>> stories =
                client.getStoriesForSprint(loginObj, 1, 10);

        assertNotNull(stories);
        assertTrue(stories.isEmpty(),
                "Empty sprint should return empty list, not null");
    }

    @Test
    void testGetStoriesForSprintHttpError() throws Exception {
        mockEndpoint("/api/v1/userstories", 403, """
                {"detail": "Authentication credentials were not provided."}
                """);

        loginObj.setAuthToken("expired-token");

        assertThrows(Exception.class,
                () -> client.getStoriesForSprint(loginObj, 1, 10),
                "Should throw on non-200 response");
    }
}