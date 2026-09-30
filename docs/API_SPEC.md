# Disaster Early-Warning Agent - API Reference Specification

## 1. Java Agent REST API (Port 8080)

### `GET /health`
Returns system heartbeat, 24/7 monitoring status, connected locations, and total cycles.
```json
{
  "status": "UP",
  "system": "Universal Disaster Early-Warning Agent",
  "timestamp": "2026-09-29T22:30:00Z",
  "monitoring_24_7": true,
  "active_locations": 4,
  "python_engine_connected": true,
  "total_cycles": 142
}
```

### `GET /api/locations`
Lists all registered dynamic locations.
```json
[
  {
    "location_id": "LOC-KAKINADA",
    "country": "India",
    "state": "Andhra Pradesh",
    "district": "Kakinada",
    "city": "Kakinada",
    "latitude": 16.9891,
    "longitude": 82.2475,
    "current_risk": 91.0,
    "risk_level": "CRITICAL"
  }
]
```

### `POST /api/locations`
Dynamically registers any global coordinate:
```json
{
  "city": "Chennai",
  "state": "Tamil Nadu",
  "district": "Chennai",
  "latitude": 13.0827,
  "longitude": 80.2707
}
```

### `GET /api/risk?location_id=LOC-KAKINADA`
Returns latest fused multi-hazard risk evaluation:
```json
{
  "location_id": "LOC-KAKINADA",
  "risk_score": 91.0,
  "risk_level": "CRITICAL",
  "confidence": 92.0,
  "trend": "Increasing",
  "predicted_risk": "CRITICAL",
  "engine_source": "PYTHON_AI",
  "contributing_factors": [
    "Heavy rainfall (>= 120 mm) exceeding critical threshold",
    "Rapid water-level increase (4.8 m)",
    "Neighboring sensor confirmation"
  ],
  "timestamp": "2026-09-29T22:30:00Z",
  "disclaimer": "Academic prototype thresholds. Not official government warning."
}
```

### `GET /api/geofence/evaluate?lat=17.0500&lon=82.2000&location_id=LOC-KAKINADA`
Evaluates a user's live position relative to the 100km / 150km boundaries:
```json
{
  "user_lat": 17.0500,
  "user_lon": 82.2000,
  "epicenter_lat": 16.9891,
  "epicenter_lon": 82.2475,
  "distance_km": 15.2,
  "geofence_status": "DISASTER_AND_WEATHER",
  "disaster_radius_km": 100.0,
  "weather_radius_km": 150.0,
  "disaster_alert_eligible": true,
  "weather_alert_eligible": true,
  "explanation": "User is 15.20 km from epicenter (<= 100 km threshold). Eligible for Critical Disaster Alert + Weather Alert."
}
```

### `GET /api/download?format=pdf|csv|json|html&location_id=LOC-KAKINADA`
Exports an emergency warning dossier in the requested format.

---

## 2. Python Risk Engine Microservice (Port 8000)

### `POST /api/risk/evaluate`
Payload:
```json
{
  "location_id": "LOC-KAKINADA",
  "sensors": {
    "rainfall": 145.0,
    "water_level": 4.8,
    "wind_speed": 62.0,
    "humidity": 94.0,
    "pressure": 988.0
  },
  "neighbor_confirmation": {
    "supported": true,
    "confirming_count": 3
  },
  "weather": {
    "rainfall": 138.0,
    "temperature": 26.0
  }
}
```
Response:
```json
{
  "risk_score": 95.6,
  "risk_level": "CRITICAL",
  "confidence": 98.0,
  "trend": "Increasing",
  "predicted_risk": "CRITICAL",
  "contributing_factors": [
    "Heavy rainfall (>= 120 mm) exceeding critical threshold",
    "Rapid water-level increase (4.8 m) approaching river crest",
    "Neighboring sensor confirmation (3 adjacent telemetry stations verified)"
  ],
  "engine_source": "PYTHON_AI"
}
```
