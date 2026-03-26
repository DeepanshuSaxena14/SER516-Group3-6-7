package edu.asu.ser516.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricRowValidationTest {

    @Test
    void rejectsBlankEntity() {
        assertThrows(IllegalArgumentException.class,
                () -> new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "   ", 1));
    }

    @Test
    void rejectsNegativeValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new MetricRow(MetricType.FAN_OUT, Scope.CLASS, "A", -1));
    }

    @Test
    void rejectsNullEnums() {
        assertThrows(NullPointerException.class,
                () -> new MetricRow(null, Scope.CLASS, "A", 1));
        assertThrows(NullPointerException.class,
                () -> new MetricRow(MetricType.FAN_OUT, null, "A", 1));
    }
}