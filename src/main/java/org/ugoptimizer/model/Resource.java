package org.ugoptimizer.model;

import java.time.LocalTime;
import java.util.Objects;

/** Immutable dispatchable security or emergency resource. */
public final class Resource {

    private final int resourceId;
    private final String resourceType;
    private final int homeLocationId;
    private final int capacity;
    private final String availabilityStatus;
    private final Integer currentLocationId;
    private final LocalTime shiftStart;
    private final LocalTime shiftEnd;

    /** Creates a resource; shift start and end must be supplied together. */
    public Resource(
            int resourceId,
            String resourceType,
            int homeLocationId,
            int capacity,
            String availabilityStatus,
            Integer currentLocationId,
            LocalTime shiftStart,
            LocalTime shiftEnd) {
        if (resourceId <= 0) {
            throw new IllegalArgumentException("resourceId must be positive");
        }
        if (homeLocationId <= 0) {
            throw new IllegalArgumentException("homeLocationId must be positive");
        }
        if (currentLocationId != null && currentLocationId <= 0) {
            throw new IllegalArgumentException("currentLocationId must be positive when present");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if ((shiftStart == null) != (shiftEnd == null)) {
            throw new IllegalArgumentException("shiftStart and shiftEnd must both be present or null");
        }

        this.resourceId = resourceId;
        this.resourceType = requiredText(resourceType, "resourceType");
        this.homeLocationId = homeLocationId;
        this.capacity = capacity;
        this.availabilityStatus = validateAvailabilityStatus(availabilityStatus);
        this.currentLocationId = currentLocationId;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public int getHomeLocationId() {
        return homeLocationId;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    /** Returns the current location ID, or {@code null} when unknown. */
    public Integer getCurrentLocationId() {
        return currentLocationId;
    }

    /** Returns the shift start, or {@code null} when no shift is recorded. */
    public LocalTime getShiftStart() {
        return shiftStart;
    }

    /** Returns the shift end, or {@code null} when no shift is recorded. */
    public LocalTime getShiftEnd() {
        return shiftEnd;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource resource)) {
            return false;
        }
        return resourceId == resource.resourceId
                && homeLocationId == resource.homeLocationId
                && capacity == resource.capacity
                && resourceType.equals(resource.resourceType)
                && availabilityStatus.equals(resource.availabilityStatus)
                && Objects.equals(currentLocationId, resource.currentLocationId)
                && Objects.equals(shiftStart, resource.shiftStart)
                && Objects.equals(shiftEnd, resource.shiftEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                resourceId, resourceType, homeLocationId, capacity, availabilityStatus,
                currentLocationId, shiftStart, shiftEnd);
    }

    @Override
    public String toString() {
        return "Resource{"
                + "resourceId=" + resourceId
                + ", resourceType='" + resourceType + '\''
                + ", homeLocationId=" + homeLocationId
                + ", capacity=" + capacity
                + ", availabilityStatus='" + availabilityStatus + '\''
                + ", currentLocationId=" + currentLocationId
                + ", shiftStart=" + shiftStart
                + ", shiftEnd=" + shiftEnd
                + '}';
    }

    private static String validateAvailabilityStatus(String value) {
        requiredText(value, "availabilityStatus");
        return switch (value) {
            case "AVAILABLE", "BUSY", "MAINTENANCE", "OFF_DUTY" -> value;
            default -> throw new IllegalArgumentException(
                    "Unsupported availabilityStatus: " + value);
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
