package edu.asu.ser516.metrics;

import java.util.Objects;

public class MetricRow {

    private final MetricType metricType;
    private final Scope scope;
    private final String entity;
    private final int value;

    private final String packageName;
    private final String filePath;

    public MetricRow(MetricType metricType,
                     Scope scope,
                     String entity,
                     int value,
                     String packageName,
                     String filePath) {

        this.metricType = Objects.requireNonNull(metricType, "metricType");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.entity = requireNonBlank(entity, "entity");

        if (value < 0) throw new IllegalArgumentException("value must be >= 0");
        this.value = value;

        this.packageName = packageName;
        this.filePath = filePath;
    }

    public MetricRow(MetricType metricType, Scope scope, String entity, int value) {
        this(metricType, scope, entity, value, null, null);
    }

    public MetricRow(String name, int value) {
        this(MetricType.FAN_OUT, Scope.CLASS, name, value, null, null);
    }

    public MetricType getMetricType() { return metricType; }
    public Scope getScope() { return scope; }
    public String getEntity() { return entity; }
    public int getValue() { return value; }
    public String getPackageName() { return packageName; }
    public String getFilePath() { return filePath; }

    public String getName() { return entity; }

    private static String requireNonBlank(String s, String fieldName) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricRow)) return false;
        MetricRow other = (MetricRow) o;
        return value == other.value
                && metricType == other.metricType
                && scope == other.scope
                && entity.equals(other.entity)
                && Objects.equals(packageName, other.packageName)
                && Objects.equals(filePath, other.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricType, scope, entity, value, packageName, filePath);
    }
}