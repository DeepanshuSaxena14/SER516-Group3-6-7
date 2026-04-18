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
}