package org.metrics;

import org.service.Metrics;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

public class MetricDbWriter {

    public static void writeAfferentEfferent(List<Metrics> metrics) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) {
            System.out.println("JDBC_URL not set — skipping DB write.");
            return;
        }

        String user = System.getenv("JDBC_USER");
        String password = System.getenv("JDBC_PASSWORD");

        String sql = "INSERT INTO afferent_efferent_result (class_name, afferent, efferent) VALUES (?, ?, ?)";

        try (Connection conn = (user != null && !user.isBlank())
                ? DriverManager.getConnection(url, user, password != null ? password : "")
                : DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Metrics m : metrics) {
                ps.setString(1, m.getClassName());
                ps.setInt(2, m.getAfferent());
                ps.setInt(3, m.getEfferent());
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Wrote " + metrics.size() + " AE rows to database.");
        } catch (Exception ex) {
            System.err.println("AE DB write failed: " + ex.getMessage());
        }
    }
}