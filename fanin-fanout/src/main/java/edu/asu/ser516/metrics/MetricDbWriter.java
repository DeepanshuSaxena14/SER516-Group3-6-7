package edu.asu.ser516.metrics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    public static void writeProjectVelocity(Map<String, Object> velocities) {
        writeProjectVelocity(velocities, null);
    }

    public static void writeProjectVelocity(Map<String, Object> velocities, Integer projectId) {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) return;
        if (velocities == null || velocities.isEmpty()) return;

        String sql = """
                INSERT INTO sprint_velocity
                    (project_id, sprint_id, sprint_name, sprint_start_date, sprint_end_date, velocity)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (project_id, sprint_id)
                DO UPDATE SET
                    sprint_name       = EXCLUDED.sprint_name,
                    sprint_start_date = EXCLUDED.sprint_start_date,
                    sprint_end_date   = EXCLUDED.sprint_end_date,
                    velocity          = EXCLUDED.velocity
                """;

        try (Connection conn = getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int rowsAdded = 0;

            for (Map.Entry<String, Object> e : velocities.entrySet()) {
                Object v = e.getValue();
                if (!(v instanceof Map)) continue;

                Map<String, Object> sprint = (Map<String, Object>) v;

                Object sprintIdObj   = sprint.get("sprintId");
                Object sprintNameObj = sprint.get("sprintName");
                Object sprintStartObj = sprint.get("sprintStart");
                Object sprintEndObj   = sprint.get("sprintEnd");
                Object velocityObj   = sprint.get("velocity");

                if (sprintIdObj == null || velocityObj == null) continue;

                Integer sprintId = (sprintIdObj instanceof Number n) ? n.intValue() : null;
                if (sprintId == null) continue;

                Double velocity = (velocityObj instanceof Number n) ? n.doubleValue() : null;
                if (velocity == null) continue;

                LocalDate sprintStart = parseDate(sprintStartObj);
                LocalDate sprintEnd   = parseDate(sprintEndObj);

                ps.setObject(1, projectId);
                ps.setInt(2, sprintId);
                ps.setString(3, sprintNameObj != null ? sprintNameObj.toString() : null);
                ps.setObject(4, sprintStart);   // java.sql driver maps LocalDate -> DATE
                ps.setObject(5, sprintEnd);
                ps.setDouble(6, velocity);
                ps.addBatch();
                rowsAdded++;
            }

            ps.executeBatch();
            System.out.println("Wrote " + rowsAdded + " sprint velocity rows to database.");

        } catch (Exception ex) {
            System.err.println("DB write failed (sprint velocity): " + ex.getMessage());
        }
    }

    private static LocalDate parseDate(Object obj) {
        if (obj == null) return null;
        try {
            return LocalDate.parse(obj.toString());  // expects YYYY-MM-DD from Taiga
        } catch (DateTimeParseException e) {
            System.err.println("Warning: could not parse date '" + obj + "': " + e.getMessage());
            return null;
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