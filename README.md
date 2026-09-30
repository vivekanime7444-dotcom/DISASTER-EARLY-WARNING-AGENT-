# Universal Disaster Early-Warning & Weather Intelligence Agent
### 24/7 Multi-Hazard Autonomous Monitoring, ADSA Sensor Graph & Dual-Radii Geofencing Platform

> **IMPORTANT DISCLAIMER:**  
> This application is an advanced academic college engineering prototype implementing **AI Rational Agent cycles**, **ADSA Sensor Graph Algorithms**, **OOPJ Java Alerting Services**, and **Python Machine Learning Risk Fusion**.  
> The risk levels (`0–20 NORMAL`, `21–40 LOW`, `41–60 MODERATE`, `61–80 HIGH`, `81–100 CRITICAL`) are **prototype research specifications** and do **NOT** replace official emergency warnings from official state disaster management authorities.

---

## 🏗️ Project Architecture & Modules

```text
DISASTER-EARLY-WARNING/
├── java-agent/                    # Core OOPJ Java Rational Agent (JDK 17)
│   ├── src/main/java/com/disaster/agent/
│   │   ├── model/                 # Sensor, Reading, Location, Zone, Weather, Alert, Risk
│   │   ├── graph/                 # ADSA Graph (BFS, DFS, Dijkstra, Neighbor Anomaly Engine)
│   │   ├── agent/                 # 8-Stage Rational Agent Cycle & Deterministic RuleEngine
│   │   ├── geo/                   # Haversine & Dual-Radii Geofence Engine (100km / 150km)
│   │   ├── client/                # Python REST Client with automatic retry & fallback
│   │   ├── weather/               # Weather Intelligence Provider & Simulator
│   │   ├── service/               # 24/7 Background Monitoring Service Worker
│   │   ├── server/                # High-Performance Java REST API Server (Port 8080)
│   │   └── DisasterAgentApp.java  # Main Application Entry Point
│   ├── src/test/java/             # Automated Verification & Boundary Test Suite
│   ├── bin/                       # Compiled bytecode
│   └── pom.xml                    # Maven Project Descriptor
├── python-risk-engine/            # Python FastAPI Risk Engine Microservice (Port 8000)
│   ├── main.py                    # REST API routes (evaluate, export, weather, health)
│   ├── risk_model.py              # Multi-hazard fusion, trends, prototype thresholds
│   ├── weather_intelligence.py    # Open-Meteo & simulated live weather engine
│   ├── export_engine.py           # Multi-format report generator (PDF, CSV, JSON, HTML)
│   └── requirements.txt
├── frontend/                      # Command Center & Citizen Emergency Dashboard
│   ├── index.html                 # Semantic HTML5 Dashboard
│   ├── css/dashboard.css          # Glassmorphic dark theme with emergency animations
│   └── js/
│       ├── app.js                 # Central controller & 24/7 background polling
│       ├── map.js                 # Leaflet map with 100km & 150km dual alert circles
│       ├── graph-vis.js           # Canvas visualizer for ADSA BFS/DFS graph traversals
│       ├── charts.js              # Chart.js precipitation & hydrograph curves
│       ├── sound.js               # Web Audio API emergency klaxon siren & chime synthesizer
│       └── export.js              # Client-side & backend PDF/CSV/JSON/HTML download triggers
├── database/                      # Relational Schema & Persistence Manager
│   ├── schema.sql                 # 11 Tables (PostgreSQL & SQLite compatible)
│   ├── init_data.sql              # Dynamic locations, zones, sensors, initial users
│   └── db_manager.py              # Connection pool & automatic bootstrap script
├── docs/                          # In-Depth Documentation
│   ├── ARCHITECTURE.md            # Topology, Rational Agent Cycle, ADSA details
│   ├── API_SPEC.md                # Full REST OpenAPI specifications
│   └── TESTING_GUIDE.md           # Step-by-step verification & boundary test runbook
├── docker/                        # Containerization
│   ├── Dockerfile.java
│   ├── Dockerfile.python
│   └── docker-compose.yml
├── run_system.bat                 # One-click Windows launcher for complete platform
├── run_tests.bat                  # One-click Windows automated test suite runner
└── README.md
```

---

## ⚡ Core Pipeline

```text
Sensors + Weather
      ↓
Data Validation
      ↓
Sensor Reliability + Anomaly Detection
      ↓
ADSA Sensor Graph
      ↓
Trend + Spatial Correlation
      ↓
Java Rational Agent
      ↓
Rule-Based Reasoning
      ↓
Python Risk Engine
      ↓
Risk Fusion + Prediction
      ↓
Affected Location Detection
      ↓
Geographic Notification Filtering
      ↓
Disaster / Weather Notification
      ↓
Dashboard + Sound + Download
      ↓
Continuous 24/7 Monitoring
```

---

## 🛰️ 8-Stage Rational Agent Cycle (OOPJ)

The Java Agent strictly executes:
$$\text{PERCEIVE} \rightarrow \text{VALIDATE} \rightarrow \text{ANALYZE} \rightarrow \text{REASON} \rightarrow \text{PREDICT} \rightarrow \text{DECIDE} \rightarrow \text{ACT} \rightarrow \text{MONITOR}$$

1. **PERCEIVE**: Ingests multi-sensor telemetry (rainfall, water level, wind speed, humidity, pressure, soil moisture, temperature) and weather signals.
2. **VALIDATE**: Filters out-of-range, missing, and stale readings (> 10 mins). Adjusts individual sensor reliability scores.
3. **ANALYZE**: Executes **ADSA Sensor Graph** neighbor cross-verification to confirm whether abnormal spikes are isolated hardware glitches or confirmed spatial hazard clusters.
4. **REASON**: Sends telemetry to Python Risk Engine via REST. If Python is offline, smoothly activates internal deterministic **Java RuleEngine** fallback.
5. **PREDICT**: Projects 24-hour hazard trajectory based on rates of change and historical baselines.
6. **DECIDE**: Determines whether risk score warrants emergency warnings or advisories.
7. **ACT**: Applies strict Haversine geofencing across dual notification radii and generates auditable delivery logs.
8. **MONITOR**: Commits time-series telemetry and updates 24/7 health matrices.

---

## 🌐 Dual Notification Radii Geofencing

The backend enforces two distinct geographic distance thresholds:
- **`DISASTER_ALERT_RADIUS = 100.0 KM`**
- **`WEATHER_ALERT_RADIUS = 150.0 KM`**

### Exact Decision Matrix:
| Distance from Epicenter ($d$) | Alert Dispatched | Audio Tone | Rationale |
| :--- | :--- | :--- | :--- |
| **$0 \le d \le 100.0$ km** | **Disaster Alert + Weather Alert** | Emergency Klaxon Siren | High hazard zone requiring evacuation/safety precautions. |
| **$100.0 < d \le 150.0$ km** | **Weather Alert ONLY** | Advisory Chime | Peripheral meteorological area. Critical disaster alert suppressed to prevent panic. |
| **$d > 150.0$ km** | **No Alert (Suppressed)** | Silent | Safe zone beyond notification boundary. |

### Exact Boundary Test Cases:
- **`100.0 km`** $\rightarrow$ Disaster Alert + Weather Alert
- **`100.1 km`** $\rightarrow$ Weather Alert ONLY
- **`150.0 km`** $\rightarrow$ Weather Alert ONLY
- **`150.1 km`** $\rightarrow$ No Alert (Suppressed)

---

## 🚀 Quick Start & Run Instructions

### Option 1: One-Click Windows Launchers
- **Launch Complete System (Python + Java + Dashboard):**
  Double-click `run_system.bat`
- **Run Automated Verification Tests:**
  Double-click `run_tests.bat`

### Option 2: Manual Terminal Commands

#### 1. Initialize Database:
```powershell
python database\db_manager.py
```

#### 2. Start Python Risk Engine (Port 8000):
```powershell
cd python-risk-engine
python main.py
```

#### 3. Start Java Rational Agent (Port 8080):
```powershell
$env:JAVA_HOME = "C:\Users\shanm\jdk-17"
$env:Path = "C:\Users\shanm\jdk-17\bin;" + $env:Path
cd java-agent
java -cp bin com.disaster.agent.DisasterAgentApp 8080
```

#### 4. Access the Dashboard:
Open your browser at **`http://localhost:8080/`** (or `http://localhost:8000/dashboard`).

---

## 🧪 Running the Verification Test Suite

To run the automated ADSA, Haversine, boundary, and failover tests:
```powershell
$env:JAVA_HOME = "C:\Users\shanm\jdk-17"
$env:Path = "C:\Users\shanm\jdk-17\bin;" + $env:Path
cd java-agent
java -ea -cp bin com.disaster.agent.AgentTestSuite
```

Expected output:
```text
====================================================================
 RUNNING DISASTER EARLY-WARNING SYSTEM AUTOMATED TEST SUITE
====================================================================
  [PASS] Test 1: ADSA Graph (BFS, DFS, Dijkstra, Connected Components)
  [PASS] Test 2: Spatial Neighbor Correlation & Anomaly Filtering
  [PASS] Test 3: Dual Notification Radii Exact Boundary Verification (100km / 100.1km / 150km / 150.1km)
  [PASS] Test 4: Java RuleEngine Risk Evaluation & Resilient Fallback
  [PASS] Test 5: Rational Agent 8-Stage Execution Cycle
====================================================================
 SUMMARY: 5 PASSED, 0 FAILED (Success Rate: 100.0%)
====================================================================
```

---

## 📦 Docker Deployment

```bash
cd docker
docker-compose up --build
```
- Command Center: `http://localhost:8080`
- Python Microservice: `http://localhost:8000`
