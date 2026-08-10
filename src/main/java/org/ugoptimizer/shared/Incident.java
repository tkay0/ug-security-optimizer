package org.ugoptimizer.shared;

/**
 * Represents a campus security incident requiring response resources.
 * Shared model — reuse existing if already defined in the project.
 */
public final class Incident {
    private final int id;
    private final String description;
    private final String location;
    private final int responseCost;   // e.g., fuel, time, or budget required
    private final int severity;       // priority / severity score (higher = more critical)

    public Incident(int id, String description, String location, int responseCost, int severity) {
        if (responseCost < 0 || severity < 0) {
            throw new IllegalArgumentException("Response cost and severity must be non-negative");
        }
        this.id = id;
        this.description = description;
        this.location = location;
        this.responseCost = responseCost;
        this.severity = severity;
    }

    public int getId() { return id; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public int getResponseCost() { return responseCost; }
    public int getSeverity() { return severity; }

    @Override
    public String toString() {
        return String.format("Incident[%d: %s @ %s | cost=%d, severity=%d]",
            id, description, location, responseCost, severity);
    }
}
