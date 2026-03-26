package edu.asu.ser516.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReferenceAdapters {

    private ReferenceAdapters() {
    }

    public static List<ClassReference> toEdges(
            Map<String, Set<String>> outgoing) {

        List<ClassReference> edges = new ArrayList<>();

        if (outgoing == null) {
            return edges;
        }

        for (Map.Entry<String, Set<String>> entry : outgoing.entrySet()) {

            String source = entry.getKey();
            if (source == null) {
                continue;
            }

            Set<String> targets = entry.getValue();
            if (targets == null) {
                continue;
            }

            for (String target : targets) {
                if (target == null) {
                    continue;
                }

                edges.add(new ClassReference(source, target));
            }
        }

        return edges;
    }
}