package org.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricsTest {

    @Test
    void storesMetricValuesCorrectly() {
        Metrics metrics = new Metrics("com.example.Sample", 2, 3);

        assertEquals("com.example.Sample", metrics.getClassName());
        assertEquals(2, metrics.getAfferent());
        assertEquals(3, metrics.getEfferent());
    }
}