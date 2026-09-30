# Disaster Early-Warning Agent - Testing & Validation Guide

## 1. Automated Java Test Suite Execution

Run the comprehensive automated unit and boundary test suite:
```powershell
cd DISASTER-EARLY-WARNING\java-agent
$env:JAVA_HOME = "C:\Users\shanm\jdk-17"
$env:Path = "C:\Users\shanm\jdk-17\bin;" + $env:Path
java -ea -cp bin com.disaster.agent.AgentTestSuite
```

### Test Cases Covered:
1. **ADSA Graph**: BFS traversal, DFS traversal, Connected Components, Dijkstra shortest path.
2. **Anomaly Detection**:
   - Case A: Single sensor spikes (145mm), neighbors read 5mm -> Flagged as **Isolated Glitch** (confidence decayed, no disaster alert).
   - Case B: Single sensor spikes (145mm), neighbors confirm 138mm and 4.8m water level -> Flagged as **Confirmed Cluster** (confidence boosted, disaster alert triggered).
3. **Dual Notification Radii Exact Boundary Verification**:
   - `100.0 km` $\rightarrow$ **Disaster Alert + Weather Alert**
   - `100.1 km` $\rightarrow$ **Weather Alert ONLY**
   - `150.0 km` $\rightarrow$ **Weather Alert ONLY**
   - `150.1 km` $\rightarrow$ **No Alert (Suppressed)**
4. **Resilience & Fallback**: Confirms Java `RuleEngine` takes over smoothly when Python service is offline.
5. **Rational Agent Cycle**: Validates the 8-stage execution cycle `PERCEIVE → VALIDATE → ANALYZE → REASON → PREDICT → DECIDE → ACT → MONITOR`.

---

## 2. Interactive Boundary Distance Verification (Browser)

1. Open `http://localhost:8080` (or `frontend/index.html`).
2. Navigate to the **User Dashboard** tab.
3. Locate the **"Simulate Your Distance to Disaster Epicenter"** slider.
4. Move the slider to test exact boundary points:
   - **`15.0 km`**: Observe red banner: **"🚨 CRITICAL DISASTER ALERT"**, dual alert badges, siren alarm.
   - **`100.0 km`**: Banner indicates inside 100km boundary (Disaster + Weather).
   - **`100.1 km`**: Observe immediate transition to blue banner: **"🌧 WEATHER ALERT ONLY"** (disaster warning suppressed).
   - **`150.0 km`**: Banner confirms inside 150km weather boundary.
   - **`150.1 km`**: Observe transition to green banner: **"🛡️ SAFE ZONE"** (beyond alert boundaries).

---

## 3. Resilience & Failure Handling Tests

### Test A: Python Risk Engine Failover
1. Stop the Python microservice (port 8000).
2. Trigger the `CRITICAL` simulation button on the dashboard.
3. Observe Java Agent console and dashboard:
   - Log shows: `Python Risk Engine currently offline. Smoothly activating Java RuleEngine fallback.`
   - Composite risk score is computed deterministically via Java rules.
   - Intelligence source displays `JAVA_FALLBACK_RULES`.
   - Alert notifications are dispatched with zero downtime!

### Test B: External Weather API Offline
1. Disconnect network or provide invalid API base URL.
2. Observe system continues uninterrupted using local deterministic meteorological simulation with accurate timestamps.
