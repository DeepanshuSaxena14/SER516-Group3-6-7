package edu.asu.ser516.metrics;

import java.util.List;
import java.util.Map;

// calculates focus factor for a sprint
// focus factor = velocity / work capacity
// velocity = total points of closed stories
// work capacity = total points of all stories in the sprint
public final class FocusFactorService {

    private FocusFactorService() {}

    public record FocusFactorResult(
            int sprintId,
            String sprintName,
            String sprintStart,
            String sprintEnd,
            double velocity,
            double workCapacity,
            double focusFactor
    ) {}

    public static FocusFactorResult compute(
            int sprintId,
            String sprintName,
            String sprintStart,
            String sprintEnd,
            List<Map<String, Object>> stories) {

        double workCapacity = 0.0;
        double velocity = 0.0;

        for (Map<String, Object> story : stories) {
            double points = extractPoints(story);
            workCapacity += points;

            // only count closed stories toward velocity
            Object isClosed = story.get("is_closed");
            boolean closed = isClosed instanceof Boolean
                    ? (Boolean) isClosed
                    : "true".equalsIgnoreCase(String.valueOf(isClosed));

            if (closed) {
                velocity += points;
            }
        }

        double focusFactor = workCapacity > 0 ? round2(velocity / workCapacity) : 0.0;

        return new FocusFactorResult(
                sprintId,
                sprintName,
                sprintStart,
                sprintEnd,
                round2(velocity),
                round2(workCapacity),
                focusFactor
        );
    }

    private static double extractPoints(Map<String, Object> story) {
        Object val = story.get("total_points");
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
