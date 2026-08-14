package org.ugoptimizer.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable record of an application event affecting a supported entity. */
public final class AuditEvent {

    private final int eventId;
    private final String eventType;
    private final Instant timestamp;
    private final String entityType;
    private final int entityId;
    private final String actorType;
    private final String details;

    /** Creates an audit event without performing database target validation. */
    public AuditEvent(
            int eventId,
            String eventType,
            Instant timestamp,
            String entityType,
            int entityId,
            String actorType,
            String details) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("eventId must be positive");
        }
        if (entityId <= 0) {
            throw new IllegalArgumentException("entityId must be positive");
        }
        this.eventId = eventId;
        this.eventType = requiredText(eventType, "eventType");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.entityType = validateEntityType(entityType);
        this.entityId = entityId;
        this.actorType = actorType;
        this.details = details;
    }

    public int getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEntityType() {
        return entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    /** Returns the actor type, or {@code null} when it was not recorded. */
    public String getActorType() {
        return actorType;
    }

    /** Returns event details, or {@code null} when none were recorded. */
    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditEvent event)) {
            return false;
        }
        return eventId == event.eventId
                && entityId == event.entityId
                && eventType.equals(event.eventType)
                && timestamp.equals(event.timestamp)
                && entityType.equals(event.entityType)
                && Objects.equals(actorType, event.actorType)
                && Objects.equals(details, event.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, timestamp, entityType, entityId, actorType, details);
    }

    @Override
    public String toString() {
        return "AuditEvent{"
                + "eventId=" + eventId
                + ", eventType='" + eventType + '\''
                + ", timestamp=" + timestamp
                + ", entityType='" + entityType + '\''
                + ", entityId=" + entityId
                + ", actorType='" + actorType + '\''
                + ", details='" + details + '\''
                + '}';
    }

    private static String validateEntityType(String value) {
        requiredText(value, "entityType");
        return switch (value) {
            case "SERVICE_REQUEST", "RESOURCE", "ROAD" -> value;
            default -> throw new IllegalArgumentException("Unsupported entityType: " + value);
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
