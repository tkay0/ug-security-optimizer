package org.ugoptimizer.model;

import java.util.Objects;

/** Immutable road connecting two distinct campus locations. */
public final class Road {

    private final int roadId;
    private final int fromLocationId;
    private final int toLocationId;
    private final double distanceKm;
    private final double travelTimeMin;
    private final double conditionWeight;
    private final String routeLabel;
    private final String roadType;
    private final String trafficLevel;
    private final boolean blocked;

    /** Creates a road using the stored, non-derived road values. */
    public Road(
            int roadId,
            int fromLocationId,
            int toLocationId,
            double distanceKm,
            double travelTimeMin,
            double conditionWeight,
            String routeLabel,
            String roadType,
            String trafficLevel,
            boolean blocked) {
        if (roadId <= 0) {
            throw new IllegalArgumentException("roadId must be positive");
        }
        if (fromLocationId <= 0 || toLocationId <= 0) {
            throw new IllegalArgumentException("Road endpoint IDs must be positive");
        }
        if (fromLocationId == toLocationId) {
            throw new IllegalArgumentException("Road endpoints must differ");
        }
        validatePositiveFinite(distanceKm, "distanceKm");
        validatePositiveFinite(travelTimeMin, "travelTimeMin");
        validatePositiveFinite(conditionWeight, "conditionWeight");

        this.roadId = roadId;
        this.fromLocationId = fromLocationId;
        this.toLocationId = toLocationId;
        this.distanceKm = distanceKm;
        this.travelTimeMin = travelTimeMin;
        this.conditionWeight = conditionWeight;
        this.routeLabel = requiredText(routeLabel, "routeLabel");
        this.roadType = validateRoadType(roadType);
        this.trafficLevel = validateTrafficLevel(trafficLevel);
        this.blocked = blocked;
    }

    public int getRoadId() {
        return roadId;
    }

    public int getFromLocationId() {
        return fromLocationId;
    }

    public int getToLocationId() {
        return toLocationId;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getTravelTimeMin() {
        return travelTimeMin;
    }

    public double getConditionWeight() {
        return conditionWeight;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    /** Returns the controlled road type, or {@code null} when unspecified. */
    public String getRoadType() {
        return roadType;
    }

    /** Returns LOW, MODERATE, HIGH, or {@code null} when unspecified. */
    public String getTrafficLevel() {
        return trafficLevel;
    }

    public boolean isBlocked() {
        return blocked;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Road road)) {
            return false;
        }
        return roadId == road.roadId
                && fromLocationId == road.fromLocationId
                && toLocationId == road.toLocationId
                && Double.compare(distanceKm, road.distanceKm) == 0
                && Double.compare(travelTimeMin, road.travelTimeMin) == 0
                && Double.compare(conditionWeight, road.conditionWeight) == 0
                && blocked == road.blocked
                && routeLabel.equals(road.routeLabel)
                && Objects.equals(roadType, road.roadType)
                && Objects.equals(trafficLevel, road.trafficLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                roadId, fromLocationId, toLocationId, distanceKm, travelTimeMin,
                conditionWeight, routeLabel, roadType, trafficLevel, blocked);
    }

    @Override
    public String toString() {
        return "Road{"
                + "roadId=" + roadId
                + ", fromLocationId=" + fromLocationId
                + ", toLocationId=" + toLocationId
                + ", distanceKm=" + distanceKm
                + ", travelTimeMin=" + travelTimeMin
                + ", conditionWeight=" + conditionWeight
                + ", routeLabel='" + routeLabel + '\''
                + ", roadType='" + roadType + '\''
                + ", trafficLevel='" + trafficLevel + '\''
                + ", blocked=" + blocked
                + '}';
    }

    private static void validatePositiveFinite(double value, String fieldName) {
        if (!Double.isFinite(value) || value <= 0.0d) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
    }

    private static String validateRoadType(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "ACCESS_ROAD", "CAMPUS_ROAD", "MAIN_ROAD", "RESIDENTIAL_ROAD" -> value;
            default -> throw new IllegalArgumentException("Unsupported roadType: " + value);
        };
    }

    private static String validateTrafficLevel(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "LOW", "MODERATE", "HIGH" -> value;
            default -> throw new IllegalArgumentException("Unsupported trafficLevel: " + value);
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
