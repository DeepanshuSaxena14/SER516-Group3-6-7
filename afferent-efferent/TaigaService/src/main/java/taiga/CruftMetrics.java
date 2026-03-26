package org.taiga;

// cruft breakdown for a single sprint.
public record CruftMetrics(
        String sprintName,
        String startDate,
        String endDate,
        int totalStories,
        int cruftStories,
        double cruftPercentage) {

    @Override
    public String toString() {
        return String.format(
                "Sprint: %s | Period: %s to %s | Total: %d stories | Cruft: %d (%.1f%%)",
                sprintName, startDate, endDate, totalStories, cruftStories, cruftPercentage
        );
    }
}