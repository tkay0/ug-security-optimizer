package org.ugoptimizer.model;

/**
 * Shared domain model for a campus security service request (incident)
 * reported to the Emergency Response Optimizer.
 *
 * <p>Represents a request such as a medical emergency, fire alarm, theft
 * report, welfare check, night patrol request, crowd control, suspicious
 * activity, access control, security escort, road obstruction, emergency
 * transport, or CCTV fault. The category is what drives resource matching:
 * {@link #matchesResourceType(String)} maps a request category to the resource
 * type that can respond to it.</p>
 *
 * <p>The canonical category values match the {@code category} column of the
 * finalized {@code service_requests.csv} dataset.</p>
 */
public class ServiceRequest {

    /** Category for medical emergencies, handled by an ambulance. */
    public static final String TYPE_MEDICAL = "MEDICAL_EMERGENCY";
    /** Category for fire alarms, handled by a fire response unit. */
    public static final String TYPE_FIRE = "FIRE_ALARM";
    /** Category for theft reports, handled by an investigation team. */
    public static final String TYPE_THEFT = "THEFT_REPORT";
    /** Category for welfare checks, handled by a patrol officer. */
    public static final String TYPE_WELFARE_CHECK = "WELFARE_CHECK";
    /** Category for night patrol requests, handled by a motorcycle patrol. */
    public static final String TYPE_NIGHT_PATROL = "NIGHT_PATROL_REQUEST";
    /** Category for crowd control, handled by a crowd control team. */
    public static final String TYPE_CROWD_CONTROL = "CROWD_CONTROL";
    /** Category for suspicious activity, handled by a patrol officer. */
    public static final String TYPE_SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY";
    /** Category for access control, handled by a patrol officer. */
    public static final String TYPE_ACCESS_CONTROL = "ACCESS_CONTROL";
    /** Category for security escorts, handled by a patrol officer. */
    public static final String TYPE_SECURITY_ESCORT = "SECURITY_ESCORT";
    /** Category for road obstructions, handled by a patrol vehicle. */
    public static final String TYPE_ROAD_OBSTRUCTION = "ROAD_OBSTRUCTION";
    /** Category for emergency transports, handled by a rapid response team. */
    public static final String TYPE_EMERGENCY_TRANSPORT = "EMERGENCY_TRANSPORT";
    /** Category for CCTV faults, handled by a CCTV technician. */
    public static final String TYPE_CCTV_FAULT = "CCTV_FAULT";

    private String id;
    private String type;
    private String severity;
    private String location;
    private String status;
    private String timestamp;

    /**
     * Constructs a new service request.
     *
     * @param id        the unique request identifier, e.g. {@code INC001}
     * @param type      the type of request, e.g. {@link #TYPE_MEDICAL}
     * @param severity  the severity level, e.g. {@code HIGH}
     * @param location  the campus location where the request occurred
     * @param status    the current lifecycle status, e.g. {@code OPEN}
     * @param timestamp the time the request was reported
     */
    public ServiceRequest(String id, String type, String severity,
                          String location, String status, String timestamp) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.location = location;
        this.status = status;
        this.timestamp = timestamp;
    }

    /**
     * Determines whether a resource of the given type can respond to this
     * request.
     *
     * <p>The mapping used by the dispatch system follows the finalized
     * {@code service_requests.csv} dataset:</p>
     * <ul>
     *   <li>{@code MEDICAL_EMERGENCY} → {@code AMBULANCE}</li>
     *   <li>{@code FIRE_ALARM} → {@code FIRE_RESPONSE_UNIT}</li>
     *   <li>{@code THEFT_REPORT} → {@code INVESTIGATION_TEAM}</li>
     *   <li>{@code WELFARE_CHECK}, {@code SUSPICIOUS_ACTIVITY},
     *       {@code ACCESS_CONTROL}, {@code SECURITY_ESCORT} →
     *       {@code PATROL_OFFICER}</li>
     *   <li>{@code NIGHT_PATROL_REQUEST} → {@code MOTORCYCLE_PATROL}</li>
     *   <li>{@code CROWD_CONTROL} → {@code CROWD_CONTROL_TEAM}</li>
     *   <li>{@code ROAD_OBSTRUCTION} → {@code PATROL_VEHICLE}</li>
     *   <li>{@code EMERGENCY_TRANSPORT} → {@code RAPID_RESPONSE_TEAM}</li>
     *   <li>{@code CCTV_FAULT} → {@code CCTV_TECHNICIAN}</li>
     * </ul>
     * <p>Comparison is case-insensitive and trims surrounding whitespace.
     * Unknown request categories match no resource type.</p>
     *
     * @param resourceType the resource type to test; may be {@code null}
     * @return {@code true} if a resource of {@code resourceType} can respond
     */
    public boolean matchesResourceType(String resourceType) {
        if (resourceType == null || type == null) {
            return false;
        }
        String normalizedType = type.trim();
        String normalizedResourceType = resourceType.trim();
        if (matchesAny(normalizedType, TYPE_MEDICAL)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_AMBULANCE);
        }
        if (matchesAny(normalizedType, TYPE_FIRE)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_FIRE_RESPONSE_UNIT);
        }
        if (matchesAny(normalizedType, TYPE_THEFT)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_INVESTIGATION_TEAM);
        }
        if (matchesAny(normalizedType, TYPE_WELFARE_CHECK, TYPE_SUSPICIOUS_ACTIVITY,
                TYPE_ACCESS_CONTROL, TYPE_SECURITY_ESCORT)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_PATROL_OFFICER);
        }
        if (matchesAny(normalizedType, TYPE_NIGHT_PATROL)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_MOTORCYCLE_PATROL);
        }
        if (matchesAny(normalizedType, TYPE_CROWD_CONTROL)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_CROWD_CONTROL_TEAM);
        }
        if (matchesAny(normalizedType, TYPE_ROAD_OBSTRUCTION)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_PATROL_VEHICLE);
        }
        if (matchesAny(normalizedType, TYPE_EMERGENCY_TRANSPORT)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_RAPID_RESPONSE_TEAM);
        }
        if (matchesAny(normalizedType, TYPE_CCTV_FAULT)) {
            return normalizedResourceType.equalsIgnoreCase(Resource.TYPE_CCTV_TECHNICIAN);
        }
        return false;
    }

    /**
     * Checks whether {@code value} equals any of the supplied candidates,
     * ignoring case.
     *
     * @param value      the value to compare
     * @param candidates the accepted values
     * @return {@code true} if the value matches at least one candidate
     */
    private static boolean matchesAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServiceRequest that)) {
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
        return "ServiceRequest{id='" + id + '\''
                + ", type='" + type + '\''
                + ", severity='" + severity + '\''
                + ", location='" + location + '\''
                + ", status='" + status + '\''
                + ", timestamp='" + timestamp + '\''
                + '}';
    }
}
