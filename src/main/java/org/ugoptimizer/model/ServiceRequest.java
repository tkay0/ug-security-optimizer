package org.ugoptimizer.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable security or emergency service request. */
public final class ServiceRequest {

    private final int requestId;
    private final int sourceLocationId;
    private final int destinationLocationId;
    private final String category;
    private final int urgency;
    private final Instant timeSubmitted;
    private final Instant deadline;
    private final String status;
    private final String requiredResourceType;
    private final String description;

    /** Creates a service request with a deadline strictly after submission. */
    public ServiceRequest(
            int requestId,
            int sourceLocationId,
            int destinationLocationId,
            String category,
            int urgency,
            Instant timeSubmitted,
            Instant deadline,
            String status,
            String requiredResourceType,
            String description) {
        if (requestId <= 0) {
            throw new IllegalArgumentException("requestId must be positive");
        }
        if (sourceLocationId <= 0 || destinationLocationId <= 0) {
            throw new IllegalArgumentException("Request location IDs must be positive");
        }
        if (sourceLocationId == destinationLocationId) {
            throw new IllegalArgumentException("Request source and destination must differ");
        }
        if (urgency < 1 || urgency > 5) {
            throw new IllegalArgumentException("urgency must be between 1 and 5");
        }

        this.requestId = requestId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.category = validateCategory(category);
        this.urgency = urgency;
        this.timeSubmitted = Objects.requireNonNull(
                timeSubmitted, "timeSubmitted cannot be null");
        this.deadline = Objects.requireNonNull(deadline, "deadline cannot be null");
        if (!deadline.isAfter(timeSubmitted)) {
            throw new IllegalArgumentException("deadline must be after timeSubmitted");
        }
        this.status = validateStatus(status);
        this.requiredResourceType = requiredResourceType;
        this.description = description;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getSourceLocationId() {
        return sourceLocationId;
    }

    public int getDestinationLocationId() {
        return destinationLocationId;
    }

    public String getCategory() {
        return category;
    }

    public int getUrgency() {
        return urgency;
    }

    public Instant getTimeSubmitted() {
        return timeSubmitted;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public String getStatus() {
        return status;
    }

    /** Returns the requested resource type, or {@code null} when unspecified. */
    public String getRequiredResourceType() {
        return requiredResourceType;
    }

    /** Returns the request description, or {@code null} when unspecified. */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServiceRequest request)) {
            return false;
        }
        return requestId == request.requestId
                && sourceLocationId == request.sourceLocationId
                && destinationLocationId == request.destinationLocationId
                && urgency == request.urgency
                && category.equals(request.category)
                && timeSubmitted.equals(request.timeSubmitted)
                && deadline.equals(request.deadline)
                && status.equals(request.status)
                && Objects.equals(requiredResourceType, request.requiredResourceType)
                && Objects.equals(description, request.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                requestId, sourceLocationId, destinationLocationId, category, urgency,
                timeSubmitted, deadline, status, requiredResourceType, description);
    }

    @Override
    public String toString() {
        return "ServiceRequest{"
                + "requestId=" + requestId
                + ", sourceLocationId=" + sourceLocationId
                + ", destinationLocationId=" + destinationLocationId
                + ", category='" + category + '\''
                + ", urgency=" + urgency
                + ", timeSubmitted=" + timeSubmitted
                + ", deadline=" + deadline
                + ", status='" + status + '\''
                + ", requiredResourceType='" + requiredResourceType + '\''
                + ", description='" + description + '\''
                + '}';
    }

    private static String validateCategory(String value) {
        requiredText(value, "category");
        return switch (value) {
            case "ACCESS_CONTROL", "CCTV_FAULT", "CROWD_CONTROL", "EMERGENCY_TRANSPORT",
                    "FIRE_ALARM", "MEDICAL_EMERGENCY", "NIGHT_PATROL_REQUEST",
                    "ROAD_OBSTRUCTION", "SECURITY_ESCORT", "SUSPICIOUS_ACTIVITY",
                    "THEFT_REPORT", "WELFARE_CHECK" -> value;
            default -> throw new IllegalArgumentException("Unsupported category: " + value);
        };
    }

    private static String validateStatus(String value) {
        requiredText(value, "status");
        return switch (value) {
            case "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED" -> value;
            default -> throw new IllegalArgumentException("Unsupported status: " + value);
        };
    }

    private static String requiredText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}
