package edu.asu.ser516.metrics;

import java.time.LocalDate;

public record DailyBurndownPoint(
        LocalDate date,
        double remainingWork,
        double remainingValue) {
}