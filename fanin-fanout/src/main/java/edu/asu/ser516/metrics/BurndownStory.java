package edu.asu.ser516.metrics;

import java.time.LocalDate;

public record BurndownStory(
        int storyId,
        double workPoints,
        double valuePoints,
        LocalDate createdDate,
        LocalDate finishedDate) {
}