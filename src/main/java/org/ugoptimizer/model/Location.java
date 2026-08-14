package org.ugoptimizer.model;

import java.util.Objects;

/**
 * Immutable campus location represented by local schematic coordinates.
 * Coordinates are not restricted to positive values.
 */
public final class Location {

    private final int locationId;
    private final String name;
    private final String area;
    private final String locationType;
    private final int xCoord;
    private final int yCoord;
    private final String operatingHours;
    private final String sourceUrl;

    /** Creates a location using the fields stored in the canonical dataset. */
    public Location(
            int locationId,
            String name,
            String area,
            String locationType,
            int xCoord,
            int yCoord,
            String operatingHours,
            String sourceUrl) {
        if (locationId <= 0) {
            throw new IllegalArgumentException("locationId must be positive");
        }
        this.locationId = locationId;
        this.name = requiredText(name, "name");
        this.area = requiredText(area, "area");
        this.locationType = requiredText(locationType, "locationType");
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.operatingHours = operatingHours;
        this.sourceUrl = requiredText(sourceUrl, "sourceUrl");
    }

    public int getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public String getLocationType() {
        return locationType;
    }

    public int getXCoord() {
        return xCoord;
    }

    public int getYCoord() {
        return yCoord;
    }

    /** Returns the display-oriented operating hours, or {@code null} when unknown. */
    public String getOperatingHours() {
        return operatingHours;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Location location)) {
            return false;
        }
        return locationId == location.locationId
                && xCoord == location.xCoord
                && yCoord == location.yCoord
                && name.equals(location.name)
                && area.equals(location.area)
                && locationType.equals(location.locationType)
                && Objects.equals(operatingHours, location.operatingHours)
                && sourceUrl.equals(location.sourceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                locationId, name, area, locationType, xCoord, yCoord, operatingHours, sourceUrl);
    }

    @Override
    public String toString() {
        return "Location{"
                + "locationId=" + locationId
                + ", name='" + name + '\''
                + ", area='" + area + '\''
                + ", locationType='" + locationType + '\''
                + ", xCoord=" + xCoord
                + ", yCoord=" + yCoord
                + ", operatingHours='" + operatingHours + '\''
                + ", sourceUrl='" + sourceUrl + '\''
                + '}';
    }

    private static String requiredText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}
