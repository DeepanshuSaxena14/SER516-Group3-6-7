package edu.asu.ser516.metrics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;
public final class MetricDbWriter {

    private static final String SCOPE_CLASS  = "class";
    private static final String SCOPE_METHOD = "method";

    private MetricDbWriter() {}

    public static void writeFanOut(Map<String, Integer> fanOut) {
        writeFanOut(fanOut, SCOPE_CLASS);
    }

    /**
     * @param scope row scope stored in {@code fan_out_metrics.scope} (e.g. class, package, project)
     */
    public static void writeFanOut(Map<String, Integer> fanOut, String scope) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) return;

        String sql = "INSERT INTO fan_out_metrics (class_name, scope, fan_out) VALUES (?, ?, ?)";

        try (Connection conn = getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Integer> e : fanOut.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, scope);
                ps.setInt(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Wrote " + fanOut.size() + " fan-out rows to database.");

        } catch (Exception ex) {
            System.err.println("DB write failed (fan-out): " + ex.getMessage());
        }
    }
    public static void writeFanIn(Map<String, Integer> fanIn) {
        writeFanIn(fanIn, SCOPE_CLASS);
    }

    public static void writeFanIn(Map<String, Integer> fanIn, String scope) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) return;

        String sql = "INSERT INTO fan_in_metrics (class_name, scope, fan_in) VALUES (?, ?, ?)";

        try (Connection conn = getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Integer> e : fanIn.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, scope);
                ps.setInt(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Wrote " + fanIn.size() + " fan-in rows (scope=" + scope + ") to database.");

        } catch (Exception ex) {
            System.err.println("DB write failed (fan-in scope=" + scope + "): " + ex.getMessage());
        }
    }

    public static void writeProjectCapacity(Map<String, Object> capacity) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) return;

        if (capacity == null || capacity.isEmpty()) return;

        String sql = "INSERT INTO capacity (capacity) VALUES (?)";
        
        try (Connection conn = getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Object> e : capacity.entrySet()) {
                ps.setObject(1, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("Wrote " + capacity.size() + " capacity rows to database.");

        } catch (Exception ex) {
            System.err.println("DB write failed (capacity): " + ex.getMessage());
        }
    }



    private static Connection getConnection(String url) throws Exception {
        String user     = System.getenv("JDBC_USER");
        String password = System.getenv("JDBC_PASSWORD");
        return (user != null && !user.isBlank())
                ? DriverManager.getConnection(url, user, password != null ? password : "")
                : DriverManager.getConnection(url);
    }
}