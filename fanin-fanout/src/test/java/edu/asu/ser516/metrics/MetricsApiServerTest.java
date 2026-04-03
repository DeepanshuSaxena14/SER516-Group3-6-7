package edu.asu.ser516.metrics;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class MetricsApiServerTest {
    private static final String ProjectPath = Paths.get("input", "Simple-Java-Calculator", "src").toAbsolutePath()
            .toString();

    @Nested
    @DisplayName("GET /metrics/fanout")
    class FanOutPath {
        @Test
        @DisplayName("Returns HTTP 200 for a valid project path")
        void checkForValidPath() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=" + ProjectPath);
                assertEquals(200, response.code());
            });
        }

        @Test
        @DisplayName("Response Content-Type is application/json")
        void returnsJsonContentType() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=" + ProjectPath);
                assertNotNull(response.header("Content-Type"));
                assertTrue(
                        Objects.requireNonNull(response.header("Content-Type")).contains("application/json"),
                        "Expected Content-Type to contain application/json but got: "
                                + response.header("Content-Type"));
            });
        }

        @Test
        @DisplayName("Response body is a non-empty JSON array")
        void returnsNonEmptyJsonArray() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=" + ProjectPath);
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(body.trim().startsWith("["), "Response should be a JSON array");
                assertTrue(body.trim().length() > 2, "JSON array should not be empty");
            });
        }

        @Test
        @DisplayName("Response array contains 'class' and 'fanOut' keys in each entry")
        void eachEntryHasClassAndFanOutKeys() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=" + ProjectPath);
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(body.contains("\"class\""),
                        "Each entry must have a 'class' key");
                assertTrue(body.contains("\"fanOut\""),
                        "Each entry must have a 'fanOut' key");
            });
        }
    }

    @Nested
    @DisplayName("GET /metrics/fanout — error cases")
    class FanOutErrorCases {
        @Test
        @DisplayName("Returns HTTP 400 when 'path' query param is missing")
        void returns400WhenPathParamMissing() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout");
                assertEquals(400, response.code());
            });
        }

        @Test
        @DisplayName("Returns HTTP 400 when 'path' points to a non-existent directory")
        void returns400WhenPathDoesNotExist() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=/this/path/does/not/exist");
                assertEquals(400, response.code());
            });
        }

        @Test
        @DisplayName("Error response body contains a descriptive 'error' field")
        void errorResponseContainsErrorField() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=/nonexistent");
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(
                        body.contains("\"error\""),
                        "Error response must contain an 'error' field, got: " + body);
            });
        }

        @Test
        @DisplayName("Returns HTTP 400 when 'path' is an empty string")
        void returns400WhenPathIsEmpty() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanout?path=");
                assertEquals(400, response.code());
            });
        }
    }

    @Nested
    @DisplayName("Server state isolation")
    class ServerStateIsolation {
        @Test
        @DisplayName("Two consecutive fanout requests return identical results")
        void multipleFanOutRequests() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                String body1 = Objects.requireNonNull(client
                        .get("/metrics/fanout?path=" + ProjectPath)
                        .body()).string();
                String body2 = Objects.requireNonNull(client
                        .get("/metrics/fanout?path=" + ProjectPath)
                        .body()).string();
                assertEquals(body1, body2,
                        "Repeated requests for the same path must return identical results");
            });
        }

        @Test
        @DisplayName("Unknown endpoint returns HTTP 404")
        void unknownEndpointError() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/unknown");
                assertEquals(404, response.code());
            });
        }
    }

    @Nested
    @DisplayName("GET /metrics/fanin")
    class FanInPath {
        @Test
        @DisplayName("Returns HTTP 200 for a valid project path")
        void returns200ForValidPath() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin?path=" + ProjectPath);
                assertEquals(200, response.code());
            });
        }

        @Test
        @DisplayName("Response contains 'classLevel' key")
        void responseContainsClassLevelKey() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin?path=" + ProjectPath);
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(body.contains("\"classLevel\""),
                        "Response must contain 'classLevel' key, got: " + body);
            });
        }

        @Test
        @DisplayName("Response contains 'methodLevel' key")
        void responseContainsMethodLevelKey() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin?path=" + ProjectPath);
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(body.contains("\"methodLevel\""),
                        "Response must contain 'methodLevel' key, got: " + body);
            });
        }

        @Test
        @DisplayName("classLevel array contains 'class' and 'fanIn' keys")
        void classLevelEntriesHaveCorrectKeys() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin?path=" + ProjectPath);
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(body.contains("\"class\""),
                        "classLevel entries must contain 'class' key");
                assertTrue(body.contains("\"fanIn\""),
                        "classLevel entries must contain 'fanIn' key");
            });
        }

        @Test
        @DisplayName("Returns HTTP 400 when 'path' query param is missing")
        void returns400WhenPathMissing() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin");
                assertEquals(400, response.code());
            });
        }

        @Test
        @DisplayName("Returns HTTP 400 when 'path' points to a non-existent directory")
        void returns400WhenPathDoesNotExist() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin?path=/this/path/does/not/exist");
                assertEquals(400, response.code());
            });
        }

        @Test
        @DisplayName("Two consecutive fanin requests return identical results")
        void consecutiveFanInRequestsAreIdentical() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                String body1 = Objects.requireNonNull(client
                        .get("/metrics/fanin?path=" + ProjectPath)
                        .body()).string();
                String body2 = Objects.requireNonNull(client
                        .get("/metrics/fanin?path=" + ProjectPath)
                        .body()).string();
                assertEquals(body1, body2,
                        "Repeated requests for the same path must return identical results");
            });

            @Nested
            @DisplayName("GET /metrics/fanin with scope")
            class FanInScope {

                @Test
                @DisplayName("scope=class returns HTTP 200")
                void returns200ForClassScope() {
                    JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                        var response = client.get("/metrics/fanin?path=" + ProjectPath + "&scope=class");
                        assertEquals(200, response.code());
                        assertNotNull(response.body());
                        String body = response.body().string();
                        assertTrue(body.contains("\"classLevel\"") || body.contains("\"class\""));
                    });
                }

                @Test
                @DisplayName("scope=package returns HTTP 200")
                void returns200ForPackageScope() {
                    JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                        var response = client.get("/metrics/fanin?path=" + ProjectPath + "&scope=package");
                        assertEquals(200, response.code());
                    });
                }

                @Test
                @DisplayName("scope=project returns HTTP 200")
                void returns200ForProjectScope() {
                    JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                        var response = client.get("/metrics/fanin?path=" + ProjectPath + "&scope=project");
                        assertEquals(200, response.code());
                    });
                }

                @Test
                @DisplayName("invalid scope returns HTTP 400")
                void invalidScopeReturns400() {
                    JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                        var response = client.get("/metrics/fanin?path=" + ProjectPath + "&scope=banana");
                        assertEquals(400, response.code());
                    });
                }
            }
        }
    }

    @Nested
    @DisplayName("GET /metrics/fanin/methods")
    class FanInMethodsPath {
        @Test
        @DisplayName("Returns HTTP 200 for a valid project path")
        void returns200ForValidPath() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin/methods?path=" + ProjectPath);
                assertEquals(200, response.code());
            });
        }

        @Test
        @DisplayName("Response body is a JSON array with method and fanIn on each entry")
        void responseIsArrayWithMethodAndFanIn() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin/methods?path=" + ProjectPath);
                assertNotNull(response.body());
                String body = response.body().string();
                assertTrue(body.trim().startsWith("["), "Response should be a JSON array");
                assertTrue(body.contains("\"method\""),
                        "Each entry must have a 'method' key");
                assertTrue(body.contains("\"fanIn\""),
                        "Each entry must have a 'fanIn' key");
            });
        }

        @Test
        @DisplayName("Returns HTTP 400 when 'path' query param is missing")
        void returns400WhenPathMissing() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                var response = client.get("/metrics/fanin/methods");
                assertEquals(400, response.code());
            });
        }

        @Test
        @DisplayName("Two consecutive requests return identical results")
        void consecutiveRequestsAreIdentical() {
            JavalinTest.test(MetricsApiServer.create(), (server, client) -> {
                String body1 = Objects.requireNonNull(client
                        .get("/metrics/fanin/methods?path=" + ProjectPath)
                        .body()).string();
                String body2 = Objects.requireNonNull(client
                        .get("/metrics/fanin/methods?path=" + ProjectPath)
                        .body()).string();
                assertEquals(body1, body2);
            });
        }
    }
}
