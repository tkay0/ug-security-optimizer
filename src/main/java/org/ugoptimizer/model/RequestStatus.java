package org.ugoptimizer.model;

public enum RequestStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public static RequestStatus fromString(String value) {
        if (value == null) return null;
        return switch (value) {
            case "PENDING" -> PENDING;
            case "ASSIGNED" -> ASSIGNED;
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "COMPLETED" -> COMPLETED;
            case "CANCELLED" -> CANCELLED;
            default -> throw new IllegalArgumentException("Unknown request status: " + value);
        };
    }

    @Override
    public String toString() {
        return name();
    }
}
