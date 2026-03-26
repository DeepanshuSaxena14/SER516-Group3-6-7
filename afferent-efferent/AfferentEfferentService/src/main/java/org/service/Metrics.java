package org.service;

public class Metrics {

    private final String className;

    private final int afferent;
    private final int efferent;

    public Metrics(String className, int afferent, int efferent) {
        this.className = className;
        this.afferent = afferent;
        this.efferent = efferent;
    }
    @Override
    public String toString() {
        return "Class: " + className +
                " | Afferents: " + afferent +
                " | Efferents: " + efferent;
    }
}
