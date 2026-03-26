package org.metrics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MetricCollector {

    public List<MetricRecord> collectMetrics() {

        List<MetricRecord> records = new ArrayList<>();

        String today = LocalDate.now().toString();

        // sample metric values
        records.add(new MetricRecord(today, "Cruft", 25.0));
        records.add(new MetricRecord(today, "DeliveryOnTime", 80.0));

        return records;
    }

}