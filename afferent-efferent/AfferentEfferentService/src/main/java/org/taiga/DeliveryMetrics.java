package org.taiga;

public record DeliveryMetrics(String sprintName, int totalTasks, int onTime, int late) {
    @Override
    public String toString() {
        return "Sprint: " + sprintName + " | Total Tasks: " + totalTasks + " | On Time: " + onTime + " | Late: " + late;
    }
}