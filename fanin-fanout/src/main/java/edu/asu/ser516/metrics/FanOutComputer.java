package edu.asu.ser516.metrics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Converts raw class references into fan-out values per class.
 * For each class, computes the distinct count of other project classes it references.
 */
public final class FanOutComputer {

    private FanOutComputer() {
    }

    /**
     * Computes fan-out for each class from raw references.
     * Fan-out = number of distinct other project classes that a class references.
     * Self-references are excluded from the count.
     *
     * @param rawReferences collection of (sourceClass, targetClass) references
     * @return map from class name to its fan-out (distinct count of referenced classes)
     */
    public static Map<String, Integer> computeFanOut(Collection<ClassReference> rawReferences) {
        Map<String, Set<String>> referencesPerClass = new HashMap<>();

        for (ClassReference ref : rawReferences) {
            String source = ref.sourceClass();
            String target = ref.targetClass();

            // Exclude self-references
            if (source.equals(target)) {
                continue;
            }

            referencesPerClass
                    .computeIfAbsent(source, k -> new HashSet<>())
                    .add(target);
        }

        Map<String, Integer> fanOut = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : referencesPerClass.entrySet()) {
            fanOut.put(entry.getKey(), entry.getValue().size());
        }

        return fanOut;
    }

    /**
     * Computes the project-level Fan-Out — a single aggregate integer equal to
     * the sum of all class-level Fan-Out values.
     *
     * <p>This is the coarsest scope: it collapses the per-class map down to one
     * number representing the total coupling weight across the entire project.</p>
     *
     * @param rawReferences collection of (sourceClass, targetClass) references
     * @return sum of all class fan-out values (0 if no external references)
     */
    public static int computeProjectFanOut(Collection<ClassReference> rawReferences) {
        return computeFanOut(rawReferences).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
}
