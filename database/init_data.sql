-- Seed Data for Universal Disaster Early-Warning Agent
-- Note: Cities are dynamically registered examples. Any lat/lon can be added dynamically.

-- 1. Locations
INSERT INTO locations (location_id, country, state, district, city, latitude, longitude, elevation_meters, population)
VALUES 
('LOC-KAKINADA', 'India', 'Andhra Pradesh', 'Kakinada', 'Kakinada', 16.9891, 82.2475, 4.0, 443000),
('LOC-VIZAG', 'India', 'Andhra Pradesh', 'Visakhapatnam', 'Visakhapatnam', 17.6868, 83.2185, 12.0, 2035000),
('LOC-VIJAYAWADA', 'India', 'Andhra Pradesh', 'NTR', 'Vijayawada', 16.5062, 80.6480, 11.0, 1048000),
('LOC-HYDERABAD', 'India', 'Telangana', 'Hyderabad', 'Hyderabad', 17.3850, 78.4867, 542.0, 6809000);

-- 2. Zones
INSERT INTO zones (zone_id, location_id, zone_name, zone_type, latitude, longitude, radius_km, vulnerability_index)
VALUES 
('ZONE-KKD-COAST', 'LOC-KAKINADA', 'Kakinada Port & Coastal Zone', 'COASTAL_SECTOR', 16.9950, 82.2600, 6.0, 0.85),
('ZONE-KKD-RIVER', 'LOC-KAKINADA', 'Medaline River Drain Basin', 'RIVER_BASIN', 16.9800, 82.2350, 4.5, 0.75),
('ZONE-KKD-URBAN', 'LOC-KAKINADA', 'Town Hall Low-Lying Sector', 'LOW_LYING_URBAN', 16.9880, 82.2450, 3.0, 0.70),

('ZONE-VZG-PORT', 'LOC-VIZAG', 'Gangavaram Coastal Bay', 'COASTAL_SECTOR', 17.6350, 83.2350, 7.0, 0.80),
('ZONE-VZG-HILLS', 'LOC-VIZAG', 'Kailasagiri Hill Slope Sector', 'HILL_SLOPE', 17.7480, 83.3420, 5.0, 0.65),

('ZONE-VJA-KRISHNA', 'LOC-VIJAYAWADA', 'Prakasam Barrage River Basin', 'RIVER_BASIN', 16.5080, 80.6120, 8.0, 0.90),
('ZONE-HYD-MUSI', 'LOC-HYDERABAD', 'Musi River Floodplain', 'RIVER_BASIN', 17.3750, 78.4800, 6.0, 0.78);

-- 3. Sensors (with baseline thresholds)
INSERT INTO sensors (sensor_id, zone_id, sensor_type, sensor_name, latitude, longitude, unit, min_threshold, warning_threshold, critical_threshold, reliability_score, health_status)
VALUES
-- Kakinada Coastal & River Sensors
('SNS-KKD-RAIN-01', 'ZONE-KKD-COAST', 'RAINFALL', 'Kakinada Port Rain Gauge Alpha', 16.9950, 82.2600, 'mm', 0, 70.0, 120.0, 0.98, 'HEALTHY'),
('SNS-KKD-RAIN-02', 'ZONE-KKD-RIVER', 'RAINFALL', 'Medaline Basin Rain Gauge Beta', 16.9810, 82.2360, 'mm', 0, 70.0, 120.0, 0.95, 'HEALTHY'),
('SNS-KKD-WL-01', 'ZONE-KKD-RIVER', 'WATER_LEVEL', 'Medaline Sluice Water Level Mon', 16.9805, 82.2355, 'm', 0.5, 3.5, 4.5, 0.99, 'HEALTHY'),
('SNS-KKD-WIND-01', 'ZONE-KKD-COAST', 'WIND_SPEED', 'Port Anemometer Tower A', 16.9960, 82.2610, 'km/h', 0, 50.0, 80.0, 0.96, 'HEALTHY'),
('SNS-KKD-HUMID-01', 'ZONE-KKD-URBAN', 'HUMIDITY', 'Town Centre Hygrometer Node', 16.9880, 82.2450, '%', 10, 85.0, 95.0, 0.97, 'HEALTHY'),
('SNS-KKD-SOIL-01', 'ZONE-KKD-RIVER', 'SOIL_MOISTURE', 'Canal Bank Soil Saturation Probe', 16.9790, 82.2340, '%', 5, 75.0, 90.0, 0.92, 'HEALTHY'),
('SNS-KKD-PRES-01', 'ZONE-KKD-COAST', 'PRESSURE', 'Coastal Barometric Sensor', 16.9940, 82.2590, 'hPa', 940, 995.0, 980.0, 0.99, 'HEALTHY'),

-- Visakhapatnam Sensors
('SNS-VZG-RAIN-01', 'ZONE-VZG-PORT', 'RAINFALL', 'Gangavaram Coastal Rain Gauge', 17.6350, 83.2350, 'mm', 0, 70.0, 120.0, 0.97, 'HEALTHY'),
('SNS-VZG-WIND-01', 'ZONE-VZG-PORT', 'WIND_SPEED', 'Harbour Wind Doppler Probe', 17.6360, 83.2360, 'km/h', 0, 55.0, 85.0, 0.95, 'HEALTHY'),
('SNS-VZG-WL-01', 'ZONE-VZG-PORT', 'WATER_LEVEL', 'Tidal Wave Gauge Gangavaram', 17.6340, 83.2340, 'm', 0.2, 3.2, 4.2, 0.96, 'HEALTHY'),

-- Vijayawada Sensors
('SNS-VJA-RAIN-01', 'ZONE-VJA-KRISHNA', 'RAINFALL', 'Prakasam Barrage Rain Station', 16.5080, 80.6120, 'mm', 0, 70.0, 120.0, 0.99, 'HEALTHY'),
('SNS-VJA-WL-01', 'ZONE-VJA-KRISHNA', 'WATER_LEVEL', 'Krishna River Spillway Hydro Sensor', 16.5075, 80.6115, 'm', 1.0, 4.0, 5.2, 0.98, 'HEALTHY'),

-- Hyderabad Sensors
('SNS-HYD-RAIN-01', 'ZONE-HYD-MUSI', 'RAINFALL', 'Musi River Basin Telemetry Rain Gauge', 17.3750, 78.4800, 'mm', 0, 65.0, 110.0, 0.96, 'HEALTHY'),
('SNS-HYD-WL-01', 'ZONE-HYD-MUSI', 'WATER_LEVEL', 'Chaderghat Bridge Hydro Level', 17.3760, 78.4810, 'm', 0.5, 3.0, 4.2, 0.94, 'HEALTHY');

-- 4. Initial Users
INSERT INTO users (user_id, username, password_hash, role, full_name, email, phone, latitude, longitude)
VALUES
('USR-ADMIN-01', 'command_admin', 'scrypt:32768:8:1$pbkdf2$simulated_safe_hash_admin', 'ADMIN', 'Disaster Cell Commander', 'commander@disastercell.gov.in', '+919876543210', 16.9891, 82.2475),
('USR-FIELD-01', 'officer_kakinada', 'scrypt:32768:8:1$pbkdf2$simulated_safe_hash_officer', 'DISASTER_OFFICER', 'Inspector V. Rao', 'vrao@disastercell.gov.in', '+919876543211', 16.9920, 82.2500),
('USR-CITIZEN-10KM', 'citizen_close', 'scrypt:32768:8:1$pbkdf2$simulated_safe_hash_user', 'CITIZEN', 'Ramesh Kumar (15km)', 'ramesh@example.com', '+919876543212', 17.0500, 82.2000),
('USR-CITIZEN-95KM', 'citizen_boundary100', 'scrypt:32768:8:1$pbkdf2$simulated_safe_hash_user', 'CITIZEN', 'Sita Devi (95km in Disaster Zone)', 'sita@example.com', '+919876543213', 17.4000, 82.8500),
('USR-CITIZEN-120KM', 'citizen_weather_zone', 'scrypt:32768:8:1$pbkdf2$simulated_safe_hash_user', 'CITIZEN', 'Anil Verma (120km Weather Alert Zone)', 'anil@example.com', '+919876543214', 17.6000, 83.1000),
('USR-CITIZEN-180KM', 'citizen_safe_zone', 'scrypt:32768:8:1$pbkdf2$simulated_safe_hash_user', 'CITIZEN', 'Sunita Reddy (180km Safe Beyond Alert Radius)', 'sunita@example.com', '+919876543215', 18.2000, 83.5000);

-- 5. Seed Initial Event Log
INSERT INTO event_log (log_id, event_category, log_level, component, message)
VALUES
('LOG-BOOT-001', 'AGENT_CYCLE', 'INFO', 'DisasterAgentCore', 'Universal Disaster Early-Warning Agent initialized in 24/7 monitoring mode.'),
('LOG-BOOT-002', 'GRAPH_PARTITION', 'INFO', 'SensorGraphEngine', 'ADSA Sensor Network Graph constructed with 14 telemetry nodes across 4 metropolitan sectors.');
