package edu.asu.ser516.metrics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Computes the Area Under the Curve (AUC) metric for a Taiga sprint.
 *
 * <h2>Algorithm</h2>
 * <p>We model the sprint as an ideal burn-down: on every calendar day
 * {@code d} from {@code sprintStart} to {@code sprintEnd} (inclusive) the
 * "ideal remaining work" decreases linearly from {@code totalPoints} to 0.
 * For each story we record the day it was closed (or we mark it as still open
 * on the last day).  The two AUC values computed are:
 *
 * <ul>
 *   <li><b>aucWork</b>  – trapezoidal area under the <em>actual</em>
 *       remaining-points curve (a proxy for "how much work was deferred").</li>
 *   <li><b>aucValue</b> – trapezoidal area under the <em>ideal</em>
 *       remaining-points line.</li>
 *   <li><b>aucRatio</b> – {@code aucWork / aucValue}; values &lt; 1 mean the
 *       team burned faster than the ideal.</li>
 * </ul>
 *
 * <p>If {@code totalPoints} is zero (no estimated stories), all AUC values are
 * 0.0 and the ratio is defined as 1.0 (neutral).
 */
public final class AucService {

    private AucService() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Immutable result record returned by {@link #compute}.
     */
    public record AucResult(double aucWork, double aucValue, double aucRatio) {}

    /**
     * Computes the AUC metric for the given sprint stories.
     *
     * @param stories    raw story maps from {@link TaigaClient#getStoriesForSprint};
     *                   each map may contain {@code total_points}, {@code is_closed},
     *                   and {@code finish_date} fields.
     * @param startDate  sprint start date, ISO-8601 ({@code "yyyy-MM-dd"})
     * @param endDate    sprint end date,   ISO-8601 ({@code "yyyy-MM-dd"})
     * @return an {@link AucResult} — never {@code null}
     */
    public static AucResult compute(
            List<Map<String, Object>> stories,
            String startDate,
            String endDate) {

        LocalDate start = LocalDate.parse(startDate.substring(0, 10));
        LocalDate end   = LocalDate.parse(endDate.substring(0, 10));

        if (!end.isAfter(start)) {
            // Zero-length sprint — nothing to integrate
            return new AucResult(0.0, 0.0, 1.0);
        }

        // Compute total story points planned for the sprint
        double totalPoints = 0.0;
        for (Map<String, Object> story : stories) {
            totalPoints += extractPoints(story);
        }

        if (totalPoints == 0.0) {
            return new AucResult(0.0, 0.0, 1.0);
        }

        long sprintDays = ChronoUnit.DAYS.between(start, end); // exclusive of end

        // Build a day-by-day "remaining points" curve.
        // Index 0 = start day, index sprintDays = end day.
        double[] remaining = new double[(int) sprintDays + 1];
        remaining[0] = totalPoints;

        // Copy the burn-down from the previous day and subtract stories closed on that day
        for (int i = 1; i <= sprintDays; i++) {
            LocalDate day = start.plusDays(i);
            double burned = burnedOnDay(stories, day, end);
            remaining[i] = Math.max(remaining[i - 1] - burned, 0.0);
        }

        // Trapezoidal integration of the actual remaining curve
        double aucWork = trapezoid(remaining);

        // Trapezoidal integration of the ideal straight-line from totalPoints → 0
        double[] ideal = new double[(int) sprintDays + 1];
        for (int i = 0; i <= sprintDays; i++) {
            ideal[i] = totalPoints * (1.0 - (double) i / sprintDays);
        }
        double aucValue = trapezoid(ideal);

        double aucRatio = aucValue == 0.0 ? 1.0 : aucWork / aucValue;

        return new AucResult(
                round2(aucWork),
                round2(aucValue),
                round2(aucRatio));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Points burned (stories closed) on exactly {@code day}, or on/before end for still-open. */
    private static double burnedOnDay(
            List<Map<String, Object>> stories, LocalDate day, LocalDate sprintEnd) {
        double burned = 0.0;
        for (Map<String, Object> story : stories) {
            LocalDate closedOn = closedDate(story, sprintEnd);
            if (closedOn != null && closedOn.equals(day)) {
                burned += extractPoints(story);
            }
        }
        return burned;
    }

    /**
     * Returns the date on which this story should be considered "burned":
     * its {@code finish_date} if closed within the sprint,
     * or {@code sprintEnd} (last day) if still open.
     */
    private static LocalDate closedDate(Map<String, Object> story, LocalDate sprintEnd) {
        Object isClosed = story.get("is_closed");
        boolean closed = isClosed instanceof Boolean
                ? (Boolean) isClosed
                : "true".equalsIgnoreCase(String.valueOf(isClosed));

        if (closed) {
            Object finish = story.get("finish_date");
            if (finish != null && !finish.toString().isBlank()) {
                try {
                    return LocalDate.parse(finish.toString().substring(0, 10));
                } catch (Exception ignored) { /* fall through */ }
            }
            // Closed but no finish_date recorded — treat as end of sprint
            return sprintEnd;
        }

        // Story still open at the end of the sprint — counts on the last day
        return sprintEnd;
    }

    /** Extracts {@code total_points} as a double; returns 0.0 if absent or null. */
    private static double extractPoints(Map<String, Object> story) {
        Object val = story.get("total_points");
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Trapezoidal rule: sum of (y[i] + y[i+1]) / 2 for all adjacent pairs. */
    private static double trapezoid(double[] y) {
        double area = 0.0;
        for (int i = 0; i < y.length - 1; i++) {
            area += (y[i] + y[i + 1]) / 2.0;
        }
        return area;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
