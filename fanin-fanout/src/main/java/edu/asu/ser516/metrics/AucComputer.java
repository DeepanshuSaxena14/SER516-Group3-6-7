package main.java.edu.asu.ser516.metrics;

/**
 * Utility that computes AUC values from a discrete daily burndown sequence
 * using the trapezoid rule.
 */
public final class AucComputer {

    private AucComputer() {}

    /**
     * Computes AUC for the provided daily remaining-points sequence.
     *
     * @param remaining non-null array with remaining points for each day from
     *                  sprint start (index 0) to sprint end (index n-1).
     * @return {@link AucService.AucResult} containing aucWork, aucValue and ratio.
     */
    public static AucService.AucResult computeFromRemaining(double[] remaining) {
        if (remaining == null || remaining.length <= 1) {
            return new AucService.AucResult(0.0, 0.0, 1.0);
        }

        int n = remaining.length;
        int sprintDays = n - 1;

        // trapezoid integration for actual remaining curve
        double aucWork = trapezoid(remaining);

        // ideal straight-line remaining from totalPoints -> 0
        double totalPoints = remaining[0];
        double[] ideal = new double[n];
        for (int i = 0; i < n; i++) {
            ideal[i] = totalPoints * (1.0 - ((double) i / sprintDays));
        }
        double aucValue = trapezoid(ideal);

        double aucRatio = aucValue == 0.0 ? 1.0 : aucWork / aucValue;

        return new AucService.AucResult(round2(aucWork), round2(aucValue), round2(aucRatio));
    }

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
