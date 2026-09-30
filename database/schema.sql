-- ====================================================================
-- UNIVERSAL DISASTER EARLY-WARNING & WEATHER INTELLIGENCE AGENT
-- Database Schema for PostgreSQL and SQLite compatible dialect
-- ====================================================================

-- 1. USERS: Emergency personnel, field agents, public subscribers
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'CITIZEN', -- 'ADMIN', 'DISASTER_OFFICER', 'FIELD_OPERATOR', 'CITIZEN'
    full_name VARCHAR(150),
    email VARCHAR(150),
    phone VARCHAR(30),
    latitude REAL,
    longitude REAL,
    push_token TEXT,
    notification_preferences TEXT DEFAULT '{"sound": true, "push": true, "email": false}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. LOCATIONS: Dynamic location registry (Country -> State -> District -> City/Town -> Zone)
CREATE TABLE IF NOT EXISTS locations (
    location_id VARCHAR(64) PRIMARY KEY,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    state VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    elevation_meters REAL DEFAULT 10.0,
    population INT DEFAULT 100000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. ZONES: Specific vulnerability and administrative subdivisions within locations
CREATE TABLE IF NOT EXISTS zones (
    zone_id VARCHAR(64) PRIMARY KEY,
    location_id VARCHAR(64) NOT NULL,
    zone_name VARCHAR(150) NOT NULL,
    zone_type VARCHAR(50) NOT NULL, -- 'RIVER_BASIN', 'COASTAL_SECTOR', 'LOW_LYING_URBAN', 'INDUSTRIAL', 'HILL_SLOPE'
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    radius_km REAL DEFAULT 5.0,
    vulnerability_index REAL DEFAULT 0.5, -- 0.0 (safe) to 1.0 (hyper-vulnerable)
    FOREIGN KEY (location_id) REFERENCES locations(location_id) ON DELETE CASCADE
);

-- 4. SENSORS: Multi-hazard IoT sensor nodes
CREATE TABLE IF NOT EXISTS sensors (
    sensor_id VARCHAR(64) PRIMARY KEY,
    zone_id VARCHAR(64) NOT NULL,
    sensor_type VARCHAR(50) NOT NULL, -- 'RAINFALL', 'WATER_LEVEL', 'TEMPERATURE', 'HUMIDITY', 'WIND_SPEED', 'SOIL_MOISTURE', 'PRESSURE'
    sensor_name VARCHAR(150) NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    unit VARCHAR(20) NOT NULL, -- 'mm', 'm', 'C', '%', 'km/h', 'hPa'
    min_threshold REAL,
    warning_threshold REAL,
    critical_threshold REAL,
    reliability_score REAL DEFAULT 1.0, -- 0.0 to 1.0 dynamic health score
    health_status VARCHAR(30) DEFAULT 'HEALTHY', -- 'HEALTHY', 'DEGRADED', 'FAULTY', 'OFFLINE'
    last_ping TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(zone_id) ON DELETE CASCADE
);

-- 5. READINGS: Sensor telemetry time-series with quality flags
CREATE TABLE IF NOT EXISTS readings (
    reading_id VARCHAR(64) PRIMARY KEY,
    sensor_id VARCHAR(64) NOT NULL,
    value REAL NOT NULL,
    unit VARCHAR(20) NOT NULL,
    is_valid BOOLEAN DEFAULT TRUE,
    is_anomaly BOOLEAN DEFAULT FALSE,
    validation_status VARCHAR(50) DEFAULT 'VALID', -- 'VALID', 'OUT_OF_RANGE', 'STALE', 'SUDDEN_SPIKE', 'INCONSISTENT'
    confidence_weight REAL DEFAULT 1.0,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sensor_id) REFERENCES sensors(sensor_id) ON DELETE CASCADE
);

-- 6. RISK_ASSESSMENTS: Combined reasoning from Java Agent and Python Engine
CREATE TABLE IF NOT EXISTS risk_assessments (
    assessment_id VARCHAR(64) PRIMARY KEY,
    location_id VARCHAR(64) NOT NULL,
    risk_score REAL NOT NULL, -- 0.0 to 100.0
    risk_level VARCHAR(30) NOT NULL, -- 'NORMAL', 'LOW', 'MODERATE', 'HIGH', 'CRITICAL'
    confidence REAL NOT NULL, -- 0.0 to 100.0%
    trend VARCHAR(30) NOT NULL, -- 'STABLE', 'INCREASING', 'DECREASING', 'VOLATILE'
    predicted_risk VARCHAR(30) NOT NULL,
    engine_source VARCHAR(30) NOT NULL, -- 'PYTHON_AI', 'JAVA_FALLBACK_RULES'
    contributing_factors TEXT NOT NULL, -- JSON array of factor strings
    sensor_readings_summary TEXT, -- JSON snapshot of sensor values
    weather_summary TEXT, -- JSON snapshot of weather data
    assessment_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (location_id) REFERENCES locations(location_id) ON DELETE CASCADE
);

-- 7. ALERTS: Generated emergency warnings with prototype disclaimer
CREATE TABLE IF NOT EXISTS alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    assessment_id VARCHAR(64),
    location_id VARCHAR(64) NOT NULL,
    alert_type VARCHAR(50) NOT NULL, -- 'DISASTER_ALERT' (within 100km), 'WEATHER_ALERT' (within 150km)
    hazard_title VARCHAR(150) NOT NULL,
    severity VARCHAR(30) NOT NULL, -- 'INFO', 'LOW', 'MODERATE', 'HIGH', 'CRITICAL'
    risk_score REAL NOT NULL,
    rainfall_mm REAL,
    water_level_m REAL,
    wind_kmh REAL,
    humidity_pct REAL,
    reasons_bullet TEXT NOT NULL,
    epicenter_lat REAL NOT NULL,
    epicenter_lon REAL NOT NULL,
    disaster_radius_km REAL DEFAULT 100.0,
    weather_radius_km REAL DEFAULT 150.0,
    status VARCHAR(30) DEFAULT 'ACTIVE', -- 'ACTIVE', 'RESOLVED', 'EXPIRED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (location_id) REFERENCES locations(location_id) ON DELETE CASCADE
);

-- 8. NOTIFICATIONS: Exact delivery tracking with distance and radius explanation
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    target_channel VARCHAR(30) NOT NULL, -- 'WEB_PUSH', 'AUDIO_ALARM', 'SMS', 'EMAIL', 'DASHBOARD_BANNER'
    recipient_lat REAL NOT NULL,
    recipient_lon REAL NOT NULL,
    calculated_distance_km REAL NOT NULL,
    qualifying_radius_km REAL NOT NULL, -- 100.0 for Disaster, 150.0 for Weather
    alert_type_assigned VARCHAR(50) NOT NULL, -- 'DISASTER_ALERT', 'WEATHER_ALERT', 'SUPPRESSED_OUT_OF_RANGE'
    delivery_status VARCHAR(30) DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'DELIVERED', 'ACKNOWLEDGED', 'FAILED', 'FILTERED_OUT'
    retry_count INT DEFAULT 0,
    explanation_reason TEXT NOT NULL, -- e.g., 'User is 64.2 km from epicenter (<100 km), Disaster + Weather alert dispatched'
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_id) REFERENCES alerts(alert_id) ON DELETE CASCADE
);

-- 9. WEATHER_DATA: Real-time and forecast weather telemetry
CREATE TABLE IF NOT EXISTS weather_data (
    weather_id VARCHAR(64) PRIMARY KEY,
    location_id VARCHAR(64) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- 'Open-Meteo', 'OpenWeatherMap', 'SimulatedFallback'
    temperature_c REAL NOT NULL,
    humidity_pct REAL NOT NULL,
    pressure_hpa REAL NOT NULL,
    wind_speed_kmh REAL NOT NULL,
    wind_direction_deg REAL,
    rainfall_mm REAL NOT NULL,
    forecast_summary VARCHAR(255),
    severe_warning VARCHAR(255),
    is_stale BOOLEAN DEFAULT FALSE,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (location_id) REFERENCES locations(location_id) ON DELETE CASCADE
);

-- 10. ACKNOWLEDGEMENTS: Responder / public alert confirmation audits
CREATE TABLE IF NOT EXISTS acknowledgements (
    ack_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    ack_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_lat REAL,
    user_lon REAL,
    safety_status VARCHAR(50) DEFAULT 'SAFE', -- 'SAFE', 'EVACUATING', 'NEED_RESCUE', 'IN_SHELTER'
    notes TEXT,
    FOREIGN KEY (alert_id) REFERENCES alerts(alert_id) ON DELETE CASCADE
);

-- 11. EVENT_LOG: Comprehensive audit trail for 24/7 resilience & failure recovery
CREATE TABLE IF NOT EXISTS event_log (
    log_id VARCHAR(64) PRIMARY KEY,
    event_category VARCHAR(50) NOT NULL, -- 'AGENT_CYCLE', 'SENSOR_ANOMALY', 'GRAPH_PARTITION', 'PYTHON_ENGINE', 'WEATHER_FETCH', 'NOTIFICATION_DELIVERY', 'FAILOVER'
    log_level VARCHAR(20) NOT NULL, -- 'INFO', 'WARN', 'ERROR', 'CRITICAL'
    component VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indices for rapid geo and time-series querying
CREATE INDEX IF NOT EXISTS idx_readings_sensor_ts ON readings (sensor_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_alerts_location_status ON alerts (location_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_alert_user ON notifications (alert_id, user_id);
CREATE INDEX IF NOT EXISTS idx_weather_location ON weather_data (location_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_event_log_ts ON event_log (created_at);
