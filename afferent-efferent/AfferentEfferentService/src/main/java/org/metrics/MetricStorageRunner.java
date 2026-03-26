package org.metrics;
import java.util.List;
public class MetricStorageRunner {
    public static void main(String[] args) throws Exception {
        MetricCollector collector = new MetricCollector();
        MetricStorage storage = new MetricStorage();

        List<MetricRecord> records = collector.collectMetrics();
        storage.storeAsCsv(records);

        System.out.println("Metric snapshot stored successfully.");
    }
}