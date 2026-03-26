package edu.asu.ser516.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricRowSorterTest {

    @Test
    void sortsByValueDescThenNameAsc() {
        MetricRow a10 = new MetricRow("A", 10);
        MetricRow b10 = new MetricRow("B", 10);
        MetricRow c20 = new MetricRow("C", 20);
        MetricRow d5  = new MetricRow("D", 5);

        List<MetricRow> input = List.of(a10, d5, b10, c20);

        List<MetricRow> sorted = MetricRowSorter.sort(input);

        assertEquals(List.of(c20, a10, b10, d5), sorted);
    }
}