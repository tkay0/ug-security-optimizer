package org.ugoptimizer.shared;

/**
 * Immutable result of an incident selection optimization.
 * Contains the selected incidents and aggregated metrics.
 * Shared model — reuse existing if already defined in the project.
 */
public final class ResponsePlan {
    private final Incident[] selectedIncidents;
    private final int totalCost;
    private final int totalSeverity;
    private final int capacity;

    public ResponsePlan(Incident[] selectedIncidents, int totalCost, int totalSeverity, int capacity) {
        this.selectedIncidents = selectedIncidents;
        this.totalCost = totalCost;
        this.totalSeverity = totalSeverity;
        this.capacity = capacity;
    }

    public Incident[] getSelectedIncidents() { return selectedIncidents; }
    public int getTotalCost() { return totalCost; }
    public int getTotalSeverity() { return totalSeverity; }
    public int getCapacity() { return capacity; }
    public int getCount() { return selectedIncidents.length; }

    @Override
    public String toString() {
        return String.format("ResponsePlan[count=%d, cost=%d/%d, severity=%d]",
            selectedIncidents.length, totalCost, capacity, totalSeverity);
    }
}
