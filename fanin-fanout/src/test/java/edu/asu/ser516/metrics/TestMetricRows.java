package edu.asu.ser516.metrics;

public final class TestMetricRows {
    private TestMetricRows() {}

    public static MetricRow fanOutClass(String entity, int value) {
        return new MetricRow(MetricType.FAN_OUT, Scope.CLASS, entity, value);
    }

    public static MetricRow fanInClass(String entity, int value) {
        return new MetricRow(MetricType.FAN_IN, Scope.CLASS, entity, value);
    }
}