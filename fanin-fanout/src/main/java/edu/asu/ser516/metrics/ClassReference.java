package edu.asu.ser516.metrics;

import java.util.Objects;

/**
 * Represents a single raw reference from one class to another within a project.
 * Used as input for fan-out computation.
 */
public final class ClassReference {

    private final String sourceClass;
    private final String targetClass;

    public ClassReference(String sourceClass, String targetClass) {
        this.sourceClass = Objects.requireNonNull(sourceClass, "sourceClass");
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass");
    }

    public String sourceClass() {
        return sourceClass;
    }

    public String targetClass() {
        return targetClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassReference that = (ClassReference) o;
        return sourceClass.equals(that.sourceClass) && targetClass.equals(that.targetClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceClass, targetClass);
    }
}
