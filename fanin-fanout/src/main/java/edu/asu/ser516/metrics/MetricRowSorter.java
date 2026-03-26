package edu.asu.ser516.metrics;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MetricRowSorter {

    private MetricRowSorter() {}

    public static List<MetricRow> sort(List<MetricRow> rows) {
        Objects.requireNonNull(rows, "rows");

        return rows.stream()
                .sorted(Comparator
                        .comparingInt(MetricRow::getValue).reversed()
                        .thenComparing(MetricRow::getName))
                .collect(Collectors.toList());
    }
}