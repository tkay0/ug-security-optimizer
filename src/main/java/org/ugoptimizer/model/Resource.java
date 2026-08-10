package org.ugoptimizer.model;

/**
 * Shared domain model for an emergency response resource available on campus.
 *
 * <p>Examples include security patrol officers, patrol vehicles, motorcycle
 * patrols, ambulances, first aid teams, fire response units, CCTV technicians,
 * investigation teams, crowd control teams and rapid response teams. Each
 * resource carries the attributes the greedy assignment algorithm needs:
 * availability, resource type, estimated response time and current workload.
 * Two resources are considered equal when their resource identifiers match.</p>
 *
 * <p>The canonical resource type values match the {@code resource_type} column
 * of the finalized {@code resources.csv} dataset.</p>
 */
public class Resource {

    /** Resource type for patrol officers. */
    public static final String TYPE_PATROL_OFFICER = "PATROL_OFFICER";
    /** Resource type for patrol vehicles. */
    public static final String TYPE_PATROL_VEHICLE = "PATROL_VEHICLE";
    /** Resource type for motorcycle patrols. */
    public static final String TYPE_MOTORCYCLE_PATROL = "MOTORCYCLE_PATROL";
    /** Resource type for ambulances. */
    public static final String TYPE_AMBULANCE = "AMBULANCE";
    /** Resource type for first aid teams. */
    public static final String TYPE_FIRST_AID_TEAM = "FIRST_AID_TEAM";
    /** Resource type for fire response units. */
    public static final String TYPE_FIRE_RESPONSE_UNIT = "FIRE_RESPONSE_UNIT";
    /** Resource type for CCTV technicians. */
    public static final String TYPE_CCTV_TECHNICIAN = "CCTV_TECHNICIAN";
    /** Resource type for investigation teams. */
    public static final String TYPE_INVESTIGATION_TEAM = "INVESTIGATION_TEAM";
    /** Resource type for crowd control teams. */
    public static final String TYPE_CROWD_CONTROL_TEAM = "CROWD_CONTROL_TEAM";
    /** Resource type for rapid response teams. */
    public static final String TYPE_RAPID_RESPONSE_TEAM = "RAPID_RESPONSE_TEAM";

    private String id;
    private String type;
    private String status;
    private String currentLocation;
    private boolean available;
    private int responseTime;
    private int currentWorkload;

    /**
     * Constructs a new resource.
     *
     * @param id              the unique resource identifier, e.g. {@code AMB001}
     * @param type            the type of resource, e.g. {@link #TYPE_AMBULANCE}
     * @param status          the operational status, e.g. {@code IDLE}
     * @param currentLocation the current location of the resource
     * @param available       whether the resource can be assigned right now
     * @param responseTime    the estimated response time in minutes
     * @param currentWorkload the number of active assignments carried
     */
    public Resource(String id, String type, String status, String currentLocation,
                    boolean available, int responseTime, int currentWorkload) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentLocation = currentLocation;
        this.available = available;
        this.responseTime = responseTime;
        this.currentWorkload = currentWorkload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    public int getCurrentWorkload() {
        return currentWorkload;
    }

    public void setCurrentWorkload(int currentWorkload) {
        this.currentWorkload = currentWorkload;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource that)) {
            return false;
        }
        if (id == null) {
            return that.id == null;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Resource{id='" + id + '\''
                + ", type='" + type + '\''
                + ", status='" + status + '\''
                + ", currentLocation='" + currentLocation + '\''
                + ", available=" + available
                + ", responseTime=" + responseTime
                + ", currentWorkload=" + currentWorkload
                + '}';
    }
}
