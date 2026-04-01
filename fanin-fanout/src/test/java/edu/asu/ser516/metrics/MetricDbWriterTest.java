package edu.asu.ser516.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MetricDbWriterTest {

    @Test
    @DisplayName("writeFanIn() is a no-op and does not throw when JDBC_URL is not set")
    void writeFanInNoOpWhenNoJdbcUrl() {
        // JDBC_URL is not set in test env — should silently do nothing
        assertDoesNotThrow(() ->
                MetricDbWriter.writeFanIn(Map.of("Calculator", 3, "UI", 1))
        );
    }

    @Test
    @DisplayName("writeFanOut() is a no-op and does not throw when JDBC_URL is not set")
    void writeFanOutNoOpWhenNoJdbcUrl() {
        assertDoesNotThrow(() ->
                MetricDbWriter.writeFanOut(Map.of("Calculator", 5, "UI", 2))
        );
    }

    @Test
    @DisplayName("writeFanIn() handles an empty map without throwing")
    void writeFanInWithEmptyMap() {
        assertDoesNotThrow(() ->
                MetricDbWriter.writeFanIn(Map.of())
        );
    }

    @Test
    @DisplayName("writeFanOut() handles an empty map without throwing")
    void writeFanOutWithEmptyMap() {
        assertDoesNotThrow(() ->
                MetricDbWriter.writeFanOut(Map.of())
        );
    }
}