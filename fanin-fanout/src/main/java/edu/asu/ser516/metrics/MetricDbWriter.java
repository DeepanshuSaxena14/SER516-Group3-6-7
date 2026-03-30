package edu.asu.ser516.metrics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;

/**
 * Writes fan-out metrics to PostgreSQL for Grafana.
 * Enabled when JDBC_URL (and optionally JDBC_USER, JDBC_PASSWORD) are set.
 */
public final class MetricDbWriter {

    private static final String SCOPE_CLASS = "class";

    private MetricDbWriter() {}

    /**
     * Inserts the current fan-out snapshot into fan_out_metrics.
     * Each run uses a single timestamp (DB default NOW()).
     * No-op if JDBC_URL is not set.
     *
     * @param fanOut map from class name to fan-out count
     */
    public static void writeFanOut(Map<String, Integer> fanOut) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) {
            return;
        }

        String user = System.getenv("JDBC_USER");
        String password = System.getenv("JDBC_PASSWORD");

        String sql = "INSERT INTO fan_out_metrics (class_name, scope, fan_out) VALUES (?, ?, ?)";

        try (Connection conn = user != null && !user.isBlank()
                ? DriverManager.getConnection(url, user, password != null ? password : "")
                : DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, SCOPE_CLASS);
                ps.setInt(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Wrote " + fanOut.size() + " fan-out rows to database.");
        } catch (Exception ex) {
            System.err.println("DB write failed: " + ex.getMessage());
        }
    }

    /**
     * Inserts the current fan-in snapshot into fan_in_metrics.
     * Each run uses a single timestamp (DB default NOW()).
     * No-op if JDBC_URL is not set.
     *
     * @param fanIn map from class name to fan-in count
     */
    public static void writeFanIn(Map<String, Integer> fanIn) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) {
            return;
        }

        String user = System.getenv("JDBC_USER");
        String password = System.getenv("JDBC_PASSWORD");

        String sql = "INSERT INTO fan_in_metrics (class_name, scope, fan_in) VALUES (?, ?, ?)";

        try (Connection conn = user != null && !user.isBlank()
                ? DriverManager.getConnection(url, user, password != null ? password : "")
                : DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Integer> e : fanIn.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, SCOPE_CLASS);
                ps.setInt(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Wrote " + fanIn.size() + " fan-in rows to database.");
        } catch (Exception ex) {
            System.err.println("DB write failed: " + ex.getMessage());
        }
    }
}
