package org.taiga;

// focus factor breakdown for a single sprint.
public record FocusFactorMetrics(
        String sprintName,
        double workCapacity,
        double velocity,
        double focusFactor) {

    @Override
    public String toString() {
        return String.format(
                "Sprint: %s | Work Capacity: %.1f | Velocity: %.1f | Focus Factor: %.1f%%",
                sprintName, workCapacity, velocity, focusFactor
        );
    }
}
