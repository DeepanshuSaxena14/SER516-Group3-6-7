package edu.asu.ser516.metrics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class BurndownReconstructor {

    private BurndownReconstructor() {
    }

    public static List<DailyBurndownPoint> reconstruct(
            LocalDate sprintStart,
            LocalDate sprintEnd,
            List<BurndownStory> stories) {
        if (sprintStart == null || sprintEnd == null) {
            throw new IllegalArgumentException("Sprint dates cannot be null");
        }

        if (sprintEnd.isBefore(sprintStart)) {
            throw new IllegalArgumentException("Sprint end cannot be before sprint start");
        }

        List<DailyBurndownPoint> result = new ArrayList<>();
        LocalDate current = sprintStart;

        while (!current.isAfter(sprintEnd)) {
            double remainingWork = 0.0;
            double remainingValue = 0.0;

            for (BurndownStory story : stories) {
                if (story == null) {
                    continue;
                }

                LocalDate createdDate = story.createdDate();
                LocalDate finishedDate = story.finishedDate();

                boolean existsByToday = createdDate == null || !createdDate.isAfter(current);
                boolean stillOpenToday = finishedDate == null || finishedDate.isAfter(current);

                if (existsByToday && stillOpenToday) {
                    remainingWork += story.workPoints();
                    remainingValue += story.valuePoints();
                }
            }

            result.add(new DailyBurndownPoint(current, remainingWork, remainingValue));
            current = current.plusDays(1);
        }

        return result;
    }
}