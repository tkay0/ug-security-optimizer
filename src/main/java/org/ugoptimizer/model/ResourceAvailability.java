package org.ugoptimizer.model;

public enum ResourceAvailability {
    AVAILABLE,
    BUSY,
    MAINTENANCE,
    OFF_DUTY;

    public static ResourceAvailability fromString(String value) {
        if (value == null) return null;
        return switch (value) {
            case "AVAILABLE" -> AVAILABLE;
            case "BUSY" -> BUSY;
            case "MAINTENANCE" -> MAINTENANCE;
            case "OFF_DUTY" -> OFF_DUTY;
            default -> throw new IllegalArgumentException("Unknown availability: " + value);
        };
    }

    @Override
    public String toString() {
        return name();
    }
}
