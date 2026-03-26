package org.metrics;
public class MetricRecord {

    private String date;
    private String metricName;
    private double metricValue;

    public MetricRecord(String date, String metricName, double metricValue) {
        this.date = date;
        this.metricName = metricName;
        this.metricValue = metricValue;
    }

    public String getDate() {
        return date;
    }

    public String getMetricName() {
        return metricName;
    }

    public double getMetricValue() {
        return metricValue;
    }

}