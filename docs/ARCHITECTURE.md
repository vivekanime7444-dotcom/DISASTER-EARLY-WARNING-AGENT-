# Universal Disaster Early-Warning & Weather Intelligence Agent
## Architectural Specification & Technical Design Dossier

> **ACADEMIC PROTOTYPE NOTICE:**  
> This system is designed as an advanced college prototype combining Object-Oriented Programming in Java (OOPJ), Advanced Data Structures & Algorithms (ADSA), Python Machine Learning/Risk Fusion, and Distributed Geofencing.  
> Thresholds (`0–20 NORMAL`, `21–40 LOW`, `41–60 MODERATE`, `61–80 HIGH`, `81–100 CRITICAL`) are academic research levels and **do NOT replace official government disaster-warning standards**.

---

### 1. System Topology & Pipeline

```text
Sensors + Weather
      ↓
Data Validation (Range, Staleness >10m, Spike Quality)
      ↓
Sensor Reliability Scoring + Anomaly Detection
      ↓
ADSA Sensor Graph (Adjacency Lists, Weighted Couplings)
      ↓
Trend + Spatial Correlation (Isolated Glitch vs Hazard Cluster)
      ↓
Java Rational Agent Cycle (PERCEIVE → VALIDATE → ANALYZE → REASON → PREDICT → DECIDE → ACT → MONITOR)
      ↓
Rule-Based Reasoning & Python Risk Engine Fusion (REST / JSON)
      ↓
Affected Location Detection & Dynamic Geofencing
      ↓
Geographic Notification Filtering (100 km Disaster vs 150 km Weather-Only)
      ↓
Alert Dispatch (Sound Synthesis + Web Push + PDF/CSV/HTML Export)
      ↓
Continuous 24/7 Monitoring Loop (5s Interval Worker)
```

---

### 2. The 8-Stage Rational Agent Cycle (Java OOPJ)

1. **PERCEIVE**: Ingests multi-hazard telemetry from registered IoT nodes (Rainfall, Water Level, Wind Speed, Humidity, Atmospheric Pressure, Soil Moisture, Temperature) and queries meteorological weather services.
2. **VALIDATE**: Performs range boundary validation, staleness detection (> 10 minutes), and updates individual sensor `reliability_score` (0.0 to 1.0). Faulty sensors are marked `DEGRADED` or `OFFLINE`.
3. **ANALYZE**: Leverages the **ADSA Sensor Graph** to execute spatial cross-validation. Determines whether an elevated reading is an isolated glitch or supported by adjacent nodes.
4. **REASON**: Sends fused sensor and weather telemetry to the Python Risk Engine (`POST http://localhost:8000/api/risk/evaluate`). If Python is unreachable, seamlessly activates the internal deterministic **Java RuleEngine** fallback.
5. **PREDICT**: Projects 24-hour hazard trajectory based on rate of change and historical baseline averages.
6. **DECIDE**: Evaluates if the composite risk score meets emergency thresholds (`>= 40.0` for advisory, `>= 60.0` for disaster alert) and determines whether to issue, escalate, or resolve active alerts.
7. **ACT**: Executes strict geographic notification filtering using Haversine distance, dispatching alerts to qualifying subscribers and logging distance explanations.
8. **MONITOR**: Commits telemetry snapshots, logs 24/7 audit metrics, and schedules the next evaluation cycle.

---

### 3. ADSA Sensor Network Graph

The sensor network is modeled as a weighted undirected graph $G = (V, E)$:
- **Vertices ($V$)**: Individual sensor telemetry nodes characterized by $(lat, lon)$, type, unit, thresholds, and health.
- **Edges ($E$)**: Physical couplings between sensors with weight $w \in (0, 1]$ representing physical correlation:
  - `METEOROLOGICAL_PROXIMITY`: Inverse geographic distance.
  - `RIVER_CORRIDOR`: Upstream to downstream hydrological coupling.
  - `COASTAL_SECTOR`: Tidal wave and gale wind interaction.
- **Algorithms Implemented**:
  - **Adjacency Lists**: $O(V + E)$ space complexity for rapid neighborhood queries.
  - **Breadth-First Search (BFS)**: Models concentric flood propagation waves outward from the epicenter.
  - **Depth-First Search (DFS)**: Traces river catchment basin chains from headwaters to coastal spillways.
  - **Connected Components**: Identifies disjoint geographic sub-networks.
  - **Dijkstra's Algorithm**: Determines shortest path of hazard propagation.
  - **Spatial Neighbor Correlation**:
    - *Isolated Anomaly*: If 0 neighbors confirm a reading spike, confidence is penalized ($w \le 0.35$), flagged as isolated artifact.
    - *Cluster Confirmation*: If $\ge 1$ adjacent hydrological or meteorological neighbor confirms the trend, confidence is boosted ($w \ge 0.95$) and escalated to active warning.

---

### 4. Dual Notification Radii Geofencing

The platform enforces two strict geographic distance thresholds:
- **`DISASTER_ALERT_RADIUS = 100.0 KM`**
- **`WEATHER_ALERT_RADIUS = 150.0 KM`**

#### Notification Decision Rules:
| Distance from Epicenter | Alert Category Dispatched | Audio Signal | Rationale |
| :--- | :--- | :--- | :--- |
| **$0 \le d \le 100.0$ km** | **Disaster Alert + Weather Alert** | High-pitch Emergency Klaxon Siren | High hazard zone requiring evacuation/safety precautions. |
| **$100.0 < d \le 150.0$ km** | **Weather Alert ONLY** | Gentle 2-tone Advisory Chime | Peripheral meteorological area. Critical disaster alert suppressed to prevent panic. |
| **$d > 150.0$ km** | **No Alert (Suppressed)** | Silent | Safe zone beyond notification perimeter. |

#### Exact Boundary Test Points:
- `100.0 km` $\rightarrow$ **Disaster Alert + Weather Alert**
- `100.1 km` $\rightarrow$ **Weather Alert ONLY**
- `150.0 km` $\rightarrow$ **Weather Alert ONLY**
- `150.1 km` $\rightarrow$ **No Alert (Suppressed)**

---

### 5. Multi-Layer Failure Resilience

| Potential Failure Point | System Fallback & Self-Healing Behavior |
| :--- | :--- |
| **Sensor Offline / Stale** | Node marked `DEGRADED`/`OFFLINE`. Reliability score decayed. Agent adjusts weighting to neighboring sensors. |
| **Python Risk Engine Down** | Java agent catches timeout/connection error, immediately invokes deterministic `RuleEngine.java` fallback, logs failover event, and schedules retry. |
| **Weather API Unavailable** | Gracefully switches to local meteorological intelligence simulation engine with fresh timestamps. |
| **Database Connection Lag** | In-memory concurrent circular queue preserves active alerts and notification audit records. |
