-- ============================================================
-- UG Campus Security & Emergency Response Optimizer
-- SQLite Database Schema
-- ============================================================

-- SQLite requires foreign-key enforcement to be enabled for each connection.
PRAGMA foreign_keys = ON;

-- ------------------------------------------------------------
-- Campus locations
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS locations (
    location_id     INTEGER PRIMARY KEY CHECK (location_id > 0),
    name            TEXT NOT NULL CHECK (trim(name) <> ''),
    area            TEXT NOT NULL CHECK (trim(area) <> ''),
    location_type   TEXT NOT NULL CHECK (trim(location_type) <> ''),
    x_coord         INTEGER NOT NULL,
    y_coord         INTEGER NOT NULL,
    operating_hours TEXT,
    source_url      TEXT NOT NULL CHECK (trim(source_url) <> '')
);

-- ------------------------------------------------------------
-- Undirected baseline roads
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roads (
    road_id          INTEGER PRIMARY KEY CHECK (road_id > 0),
    from_location_id INTEGER NOT NULL,
    to_location_id   INTEGER NOT NULL,
    distance_km      REAL NOT NULL CHECK (distance_km > 0),
    travel_time_min  REAL NOT NULL CHECK (travel_time_min > 0),
    condition_weight REAL NOT NULL CHECK (condition_weight > 0),
    route_label      TEXT NOT NULL CHECK (trim(route_label) <> ''),
    road_type        TEXT CHECK (
        road_type IS NULL OR road_type IN (
            'ACCESS_ROAD',
            'CAMPUS_ROAD',
            'MAIN_ROAD',
            'RESIDENTIAL_ROAD'
        )
    ),
    traffic_level    TEXT CHECK (
        traffic_level IS NULL OR traffic_level IN ('LOW', 'MODERATE', 'HIGH')
    ),
    is_blocked       INTEGER NOT NULL DEFAULT 0 CHECK (is_blocked IN (0, 1)),

    FOREIGN KEY (from_location_id)
        REFERENCES locations (location_id) ON DELETE RESTRICT,
    FOREIGN KEY (to_location_id)
        REFERENCES locations (location_id) ON DELETE RESTRICT,
    CHECK (from_location_id <> to_location_id)
);

-- ------------------------------------------------------------
-- Security and emergency service requests
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_requests (
    request_id                INTEGER PRIMARY KEY CHECK (request_id > 0),
    source_location_id        INTEGER NOT NULL,
    destination_location_id   INTEGER NOT NULL,
    category                  TEXT NOT NULL CHECK (
        category IN (
            'ACCESS_CONTROL',
            'CCTV_FAULT',
            'CROWD_CONTROL',
            'EMERGENCY_TRANSPORT',
            'FIRE_ALARM',
            'MEDICAL_EMERGENCY',
            'NIGHT_PATROL_REQUEST',
            'ROAD_OBSTRUCTION',
            'SECURITY_ESCORT',
            'SUSPICIOUS_ACTIVITY',
            'THEFT_REPORT',
            'WELFARE_CHECK'
        )
    ),
    urgency                   INTEGER NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    time_submitted            TEXT NOT NULL CHECK (julianday(time_submitted) IS NOT NULL),
    deadline                  TEXT NOT NULL CHECK (julianday(deadline) IS NOT NULL),
    status                    TEXT NOT NULL CHECK (
        status IN ('PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    required_resource_type    TEXT,
    description               TEXT,

    FOREIGN KEY (source_location_id)
        REFERENCES locations (location_id) ON DELETE RESTRICT,
    FOREIGN KEY (destination_location_id)
        REFERENCES locations (location_id) ON DELETE RESTRICT,
    CHECK (source_location_id <> destination_location_id),
    CHECK (julianday(deadline) > julianday(time_submitted))
);

-- ------------------------------------------------------------
-- Dispatchable security and emergency resources
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resources (
    resource_id          INTEGER PRIMARY KEY CHECK (resource_id > 0),
    resource_type        TEXT NOT NULL CHECK (trim(resource_type) <> ''),
    home_location_id     INTEGER NOT NULL,
    capacity             INTEGER NOT NULL CHECK (capacity > 0),
    availability_status  TEXT NOT NULL CHECK (
        availability_status IN ('AVAILABLE', 'BUSY', 'MAINTENANCE', 'OFF_DUTY')
    ),
    current_location_id  INTEGER,
    shift_start          TEXT,
    shift_end            TEXT,

    FOREIGN KEY (home_location_id)
        REFERENCES locations (location_id) ON DELETE RESTRICT,
    FOREIGN KEY (current_location_id)
        REFERENCES locations (location_id) ON DELETE RESTRICT,
    CHECK (
        (shift_start IS NULL AND shift_end IS NULL)
        OR (shift_start IS NOT NULL AND shift_end IS NOT NULL)
    )
);

-- ------------------------------------------------------------
-- Controlled road blockage and condition scenarios
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS road_scenarios (
    scenario_id                 INTEGER PRIMARY KEY CHECK (scenario_id > 0),
    scenario_name               TEXT NOT NULL CHECK (trim(scenario_name) <> ''),
    road_id                     INTEGER NOT NULL,
    route_label                 TEXT NOT NULL CHECK (trim(route_label) <> ''),
    scenario_start              TEXT NOT NULL CHECK (julianday(scenario_start) IS NOT NULL),
    scenario_end                TEXT NOT NULL CHECK (julianday(scenario_end) IS NOT NULL),
    is_blocked_override         INTEGER NOT NULL CHECK (is_blocked_override IN (0, 1)),
    condition_weight_multiplier REAL NOT NULL CHECK (condition_weight_multiplier > 0),
    travel_time_multiplier      REAL NOT NULL CHECK (travel_time_multiplier > 0),
    reason                      TEXT NOT NULL CHECK (trim(reason) <> ''),

    FOREIGN KEY (road_id)
        REFERENCES roads (road_id) ON DELETE RESTRICT,
    UNIQUE (scenario_name, road_id),
    CHECK (julianday(scenario_end) > julianday(scenario_start))
);

-- ------------------------------------------------------------
-- Application audit history
-- entity_id is intentionally polymorphic and has no normal SQL
-- foreign key. Importer/DAO code must validate it using entity_type.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_events (
    event_id        INTEGER PRIMARY KEY CHECK (event_id > 0),
    event_type      TEXT NOT NULL CHECK (trim(event_type) <> ''),
    event_timestamp TEXT NOT NULL CHECK (julianday(event_timestamp) IS NOT NULL),
    entity_type     TEXT NOT NULL CHECK (
        entity_type IN ('SERVICE_REQUEST', 'RESOURCE', 'ROAD')
    ),
    entity_id       INTEGER NOT NULL CHECK (entity_id > 0),
    actor_type      TEXT,
    details         TEXT
);

-- ------------------------------------------------------------
-- Planned and measured algorithm benchmark runs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS algorithm_runs (
    run_id           INTEGER PRIMARY KEY CHECK (run_id > 0),
    algorithm_name   TEXT NOT NULL CHECK (trim(algorithm_name) <> ''),
    input_size       INTEGER NOT NULL CHECK (input_size > 0),
    time_ns          INTEGER CHECK (time_ns IS NULL OR time_ns >= 0),
    memory_kb        REAL CHECK (memory_kb IS NULL OR memory_kb >= 0),
    date_run         TEXT CHECK (date_run IS NULL OR julianday(date_run) IS NOT NULL),
    status           TEXT NOT NULL CHECK (status IN ('PLANNED', 'MEASURED')),
    experiment_group TEXT NOT NULL CHECK (trim(experiment_group) <> ''),
    run_number       INTEGER NOT NULL CHECK (run_number > 0),

    UNIQUE (experiment_group, run_number),
    CHECK (
        (
            status = 'PLANNED'
            AND time_ns IS NULL
            AND memory_kb IS NULL
            AND date_run IS NULL
        )
        OR
        (
            status = 'MEASURED'
            AND time_ns IS NOT NULL
            AND memory_kb IS NOT NULL
            AND date_run IS NOT NULL
        )
    )
);

-- ------------------------------------------------------------
-- Query and integrity indexes
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_roads_from_location
    ON roads (from_location_id);

CREATE INDEX IF NOT EXISTS idx_roads_to_location
    ON roads (to_location_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_roads_undirected_endpoints
    ON roads (
        min(from_location_id, to_location_id),
        max(from_location_id, to_location_id)
    );

CREATE INDEX IF NOT EXISTS idx_service_requests_source
    ON service_requests (source_location_id);

CREATE INDEX IF NOT EXISTS idx_service_requests_destination
    ON service_requests (destination_location_id);

CREATE INDEX IF NOT EXISTS idx_service_requests_dispatch_queue
    ON service_requests (status, urgency DESC, time_submitted);

CREATE INDEX IF NOT EXISTS idx_resources_type_availability
    ON resources (resource_type, availability_status);

CREATE INDEX IF NOT EXISTS idx_resources_current_location
    ON resources (current_location_id);

CREATE INDEX IF NOT EXISTS idx_road_scenarios_road
    ON road_scenarios (road_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_entity_history
    ON audit_events (entity_type, entity_id, event_timestamp);

CREATE INDEX IF NOT EXISTS idx_algorithm_runs_lookup
    ON algorithm_runs (algorithm_name, input_size, status);
