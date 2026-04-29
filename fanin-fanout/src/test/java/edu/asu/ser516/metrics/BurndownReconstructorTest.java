package edu.asu.ser516.metrics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BurndownReconstructorTest {

    @Test
    void reconstructsDailyBurndownCorrectly() {
        LocalDate start = LocalDate.of(2026, 4, 10);
        LocalDate end = LocalDate.of(2026, 4, 12);

        List<BurndownStory> stories = List.of(
                new BurndownStory(
                        1,
                        5.0,
                        8.0,
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 4, 11)),
                new BurndownStory(
                        2,
                        3.0,
                        4.0,
                        LocalDate.of(2026, 4, 10),
                        null));

        List<DailyBurndownPoint> result = BurndownReconstructor.reconstruct(start, end, stories);

        assertEquals(3, result.size());

        assertEquals(8.0, result.get(0).remainingWork());
        assertEquals(12.0, result.get(0).remainingValue());

        assertEquals(3.0, result.get(1).remainingWork());
        assertEquals(4.0, result.get(1).remainingValue());

        assertEquals(3.0, result.get(2).remainingWork());
        assertEquals(4.0, result.get(2).remainingValue());
    }

    @Test
    void excludesStoriesNotCreatedYet() {
        LocalDate start = LocalDate.of(2026, 4, 10);
        LocalDate end = LocalDate.of(2026, 4, 11);

        List<BurndownStory> stories = List.of(
                new BurndownStory(
                        1,
                        5.0,
                        10.0,
                        LocalDate.of(2026, 4, 11),
                        null));

        List<DailyBurndownPoint> result = BurndownReconstructor.reconstruct(start, end, stories);

        assertEquals(0.0, result.get(0).remainingWork());
        assertEquals(0.0, result.get(0).remainingValue());

        assertEquals(5.0, result.get(1).remainingWork());
        assertEquals(10.0, result.get(1).remainingValue());
    }

    @Test
    void throwsWhenSprintDatesAreInvalid() {
        LocalDate start = LocalDate.of(2026, 4, 12);
        LocalDate end = LocalDate.of(2026, 4, 10);

        assertThrows(IllegalArgumentException.class, () -> BurndownReconstructor.reconstruct(start, end, List.of()));
    }
}