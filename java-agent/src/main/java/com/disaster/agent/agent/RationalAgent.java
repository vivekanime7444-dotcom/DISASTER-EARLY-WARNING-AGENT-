package com.disaster.agent.agent;

import com.disaster.agent.client.PythonRiskClient;
import com.disaster.agent.geo.GeoFenceEngine;
import com.disaster.agent.graph.SensorGraph;
import com.disaster.agent.model.*;
import com.disaster.agent.weather.WeatherService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * AI Rational Agent Core (OOPJ Java Implementation).
 * Strictly implements the 8-stage rational cycle:
 * PERCEIVE → VALIDATE → ANALYZE → REASON → PREDICT → DECIDE → ACT → MONITOR
 *
 * Maintains continuous state for:
 * - Current Risk Score & Level
 * - Predicted Risk & Confidence
 * - Environmental Trends
 * - Sensor Health & Reliability Matrix
 * - Weather Intelligence
 * - Affected Locations & Dynamic Geofences
 * - Active Alerts & Notification History
 */
public class RationalAgent {
    private static final Logger LOGGER = Logger.getLogger(RationalAgent.class.getName());

    private final String locationId;
    private final Location location;
    private final SensorGraph sensorGraph;
    private final WeatherService weatherService;
    private final PythonRiskClient pythonRiskClient;

    // Agent Internal State
    private RiskAssessment currentAssessment;
    private WeatherData currentWeather;
    private final List<Alert> activeAlerts = new ArrayList<>();
    private final List<NotificationResult> notificationLog = new ArrayList<>();
    private final Map<String, SensorNode> registeredSensors = new ConcurrentHashMap<>();
    private double historicalBaselineAvgRainfall = 25.0;
    private long totalCyclesExecuted = 0;
    private Instant lastCycleTimestamp;

    public RationalAgent(Location location, SensorGraph sensorGraph,
                         WeatherService weatherService, PythonRiskClient pythonRiskClient) {
        this.location = location;
        this.locationId = location.getLocationId();
        this.sensorGraph = sensorGraph;
        this.weatherService = weatherService;
        this.pythonRiskClient = pythonRiskClient;
    }

    public void registerSensor(SensorNode sensor) {
        registeredSensors.put(sensor.getSensorId(), sensor);
        sensorGraph.addNode(sensor);
    }

    /**
     * Executes one complete iteration of the Rational Agent Cycle:
     * PERCEIVE → VALIDATE → ANALYZE → REASON → PREDICT → DECIDE → ACT → MONITOR
     */
    public synchronized AgentCycleReport executeCycle() {
        totalCyclesExecuted++;
        lastCycleTimestamp = Instant.now();

        // 1. PERCEIVE: Ingest current sensor telemetry and weather signals
        Map<String, Reading> rawReadings = stagePerceive();
        WeatherData weather = weatherService.getWeather(location);
        this.currentWeather = weather;

        // 2. VALIDATE: Detect missing, stale, abnormal, and out-of-range sensor readings
        Map<SensorType, Double> validatedValues = stageValidate(rawReadings);

        // 3. ANALYZE: ADSA Graph neighboring-sensor spatial correlation and clustering
        SensorGraph.NeighborCorrelationResult correlation = stageAnalyze(validatedValues);

        // 4. REASON: Risk Fusion via Python Risk Engine (with Java Rule fallback)
        RiskAssessment assessment = stageReason(validatedValues, correlation, weather);
        this.currentAssessment = assessment;

        // 5. PREDICT: Trajectory projection & trend modeling (incorporated in assessment)
        Trend trend = assessment.getTrend();
        RiskLevel predictedRisk = assessment.getPredictedRisk();

        // 6. DECIDE: Determine whether emergency warning / advisory is triggered
        Alert alertToIssue = stageDecide(assessment, validatedValues, weather);

        // 7. ACT: Issue alert, execute geographic notification filtering (100km vs 150km)
        List<NotificationResult> cycleNotifications = stageAct(alertToIssue);

        // 8. MONITOR: Update telemetry history, health matrix, and cycle metrics
        stageMonitor(assessment, cycleNotifications);

        return new AgentCycleReport(
            totalCyclesExecuted,
            locationId,
            location.getCity(),
            assessment.getRiskScore(),
            assessment.getRiskLevel(),
            assessment.getConfidence(),
            trend,
            predictedRisk,
            assessment.getEngineSource(),
            assessment.getContributingFactors(),
            alertToIssue != null ? alertToIssue.getAlertId() : null,
            cycleNotifications.size(),
            lastCycleTimestamp
        );
    }

    /**
     * Stage 1: PERCEIVE
     */
    private Map<String, Reading> stagePerceive() {
        Map<String, Reading> readings = new HashMap<>();
        for (SensorNode sensor : registeredSensors.values()) {
            Reading latest = sensor.getLatestReading();
            if (latest != null) {
                readings.put(sensor.getSensorId(), latest);
            }
        }
        return readings;
    }

    /**
     * Stage 2: VALIDATE
     */
    private Map<SensorType, Double> stageValidate(Map<String, Reading> rawReadings) {
        Map<SensorType, Double> valuesByType = new EnumMap<>(SensorType.class);
        Map<SensorType, Integer> countByType = new EnumMap<>(SensorType.class);

        Instant now = Instant.now();

        for (SensorNode sensor : registeredSensors.values()) {
            Reading r = rawReadings.get(sensor.getSensorId());

            if (r == null) {
                // Missing reading
                sensor.setHealthStatus(HealthStatus.DEGRADED);
                sensor.setReliabilityScore(sensor.getReliabilityScore() - 0.05);
                continue;
            }

            // Check staleness (older than 10 minutes)
            if (now.getEpochSecond() - r.getTimestamp().getEpochSecond() > 600) {
                r.setValidationStatus(ValidationStatus.STALE);
                r.setValid(false);
                sensor.setHealthStatus(HealthStatus.DEGRADED);
                sensor.setReliabilityScore(sensor.getReliabilityScore() - 0.03);
                continue;
            }

            // Check hard range boundaries
            if (r.getValue() < 0 && sensor.getSensorType() != SensorType.TEMPERATURE) {
                r.setValidationStatus(ValidationStatus.OUT_OF_RANGE);
                r.setValid(false);
                sensor.setReliabilityScore(sensor.getReliabilityScore() - 0.1);
                continue;
            }

            // Valid reading: restore sensor health gradually
            r.setValid(true);
            r.setValidationStatus(ValidationStatus.VALID);
            sensor.setHealthStatus(HealthStatus.HEALTHY);
            sensor.setReliabilityScore(Math.min(1.0, sensor.getReliabilityScore() + 0.02));

            // Accumulate weighted reading for sensor type
            double weightedVal = r.getValue() * sensor.getReliabilityScore();
            valuesByType.merge(sensor.getSensorType(), weightedVal, Double::sum);
            countByType.merge(sensor.getSensorType(), 1, Integer::sum);
        }

        // Compute averages
        for (Map.Entry<SensorType, Integer> entry : countByType.entrySet()) {
            SensorType type = entry.getKey();
            int count = entry.getValue();
            if (count > 0) {
                valuesByType.put(type, valuesByType.get(type) / count);
            }
        }

        return valuesByType;
    }

    /**
     * Stage 3: ANALYZE (ADSA Sensor Graph spatial neighbor correlation)
     */
    private SensorGraph.NeighborCorrelationResult stageAnalyze(Map<SensorType, Double> values) {
        // Find most critical sensor node in this location
        SensorNode focalNode = null;
        double maxSeverityRatio = 0.0;

        for (SensorNode node : registeredSensors.values()) {
            Reading r = node.getLatestReading();
            if (r != null && node.getWarningThreshold() > 0) {
                double ratio = r.getValue() / node.getWarningThreshold();
                if (ratio > maxSeverityRatio) {
                    maxSeverityRatio = ratio;
                    focalNode = node;
                }
            }
        }

        if (focalNode != null && focalNode.getLatestReading() != null) {
            return sensorGraph.validateWithNeighbors(focalNode.getSensorId(), focalNode.getLatestReading());
        }

        return new SensorGraph.NeighborCorrelationResult(true, 0, 0, "All local sensors operating nominally", 1.0);
    }

    /**
     * Stage 4: REASON (Fuses data via Python Risk Client with fallback)
     */
    private RiskAssessment stageReason(Map<SensorType, Double> values,
                                       SensorGraph.NeighborCorrelationResult correlation,
                                       WeatherData weather) {
        return pythonRiskClient.evaluateRiskWithFallback(
                locationId, values, correlation, weather, historicalBaselineAvgRainfall
        );
    }

    /**
     * Stage 6: DECIDE (Determine whether Alert is warranted)
     */
    private Alert stageDecide(RiskAssessment assessment, Map<SensorType, Double> values, WeatherData weather) {
        // Academic prototype threshold: riskScore >= 40 triggers alerts/advisories
        if (assessment.getRiskScore() < 40.0) {
            // Resolve previous alerts if any
            for (Alert a : activeAlerts) {
                a.setStatus("RESOLVED");
            }
            return null;
        }

        String hazardTitle;
        if (values.getOrDefault(SensorType.RAINFALL, 0.0) >= 70.0 || values.getOrDefault(SensorType.WATER_LEVEL, 0.0) >= 3.2) {
            hazardTitle = assessment.getRiskLevel() == RiskLevel.CRITICAL ? "Flash Flood & Inundation Emergency" : "Flood Warning Advisory";
        } else if (values.getOrDefault(SensorType.WIND_SPEED, 0.0) >= 50.0 || values.getOrDefault(SensorType.PRESSURE, 1013.0) <= 990.0) {
            hazardTitle = "Severe Tropical Gale / Cyclone Alert";
        } else if (values.getOrDefault(SensorType.TEMPERATURE, 25.0) >= 42.0) {
            hazardTitle = "Extreme Heatwave Warning";
        } else {
            hazardTitle = "Multi-Hazard Environmental Risk";
        }

        Alert alert = new Alert(
            "ALT-" + locationId + "-" + System.currentTimeMillis(),
            assessment.getAssessmentId(),
            locationId,
            location.getCity() + ", " + location.getDistrict(),
            location.getDistrict(),
            assessment.getRiskScore() >= 60.0 ? "DISASTER_ALERT" : "WEATHER_ALERT",
            hazardTitle,
            assessment.getRiskLevel(),
            assessment.getRiskScore(),
            values.getOrDefault(SensorType.RAINFALL, weather != null ? weather.getRainfallMm() : 0.0),
            values.getOrDefault(SensorType.WATER_LEVEL, 1.2),
            values.getOrDefault(SensorType.WIND_SPEED, weather != null ? weather.getWindSpeedKmh() : 15.0),
            values.getOrDefault(SensorType.HUMIDITY, weather != null ? weather.getHumidityPct() : 75.0),
            values.getOrDefault(SensorType.TEMPERATURE, weather != null ? weather.getTemperatureC() : 28.0),
            assessment.getConfidence(),
            assessment.getTrend(),
            assessment.getPredictedRisk(),
            assessment.getContributingFactors(),
            location.getLatitude(),
            location.getLongitude(),
            weather != null ? weather.getForecastSummary() : "Heavy precipitation expected",
            "ACTIVE",
            Instant.now()
        );

        activeAlerts.add(0, alert);
        if (activeAlerts.size() > 20) activeAlerts.remove(activeAlerts.size() - 1);
        return alert;
    }

    /**
     * Stage 7: ACT (Geographic notification filtering for subscribers)
     * Strictly enforces: Only alerts sent in user alert district.
     */
    private List<NotificationResult> stageAct(Alert alert) {
        List<NotificationResult> results = new ArrayList<>();
        if (alert == null) return results;

        // Sample test subscriber coordinates at representative distances
        // (including critical boundary test points: 15km, 95km, 100km, 100.1km, 120km, 150km, 150.1km, 180km, and neighboring district)
        List<SubscriberPoint> subscribers = createTestSubscribers(location.getLatitude(), location.getLongitude(), location.getDistrict());

        for (SubscriberPoint sub : subscribers) {
            NotificationResult notif = GeoFenceEngine.createNotificationResultWithDistrict(
                alert.getAlertId(),
                sub.userId,
                "WEB_PUSH_AND_AUDIO",
                alert.getEpicenterLat(),
                alert.getEpicenterLon(),
                alert.getDistrict(),
                sub.latitude,
                sub.longitude,
                sub.district
            );
            results.add(notif);
            notificationLog.add(0, notif);
        }

        if (notificationLog.size() > 100) {
            notificationLog.subList(100, notificationLog.size()).clear();
        }

        return results;
    }

    /**
     * Stage 8: MONITOR (Update baselines and telemetry health)
     */
    private void stageMonitor(RiskAssessment assessment, List<NotificationResult> notifications) {
        // Continuous 24/7 logging
        LOGGER.info(String.format("[24/7 MONITOR] Cycle #%d | Loc: %s (%s) | Risk: %.1f (%s) | Conf: %.0f%% | Source: %s | Active Alerts: %d",
                totalCyclesExecuted, location.getCity(), location.getDistrict(), assessment.getRiskScore(), assessment.getRiskLevel(),
                assessment.getConfidence(), assessment.getEngineSource(), activeAlerts.size()));
    }

    private List<SubscriberPoint> createTestSubscribers(double epicLat, double epicLon, String district) {
        List<SubscriberPoint> list = new ArrayList<>();
        // 1. Close urban center (approx 15 km) inside alert district
        double[] pt15 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 15.0, 45.0);
        list.add(new SubscriberPoint("SUB-15KM-DISASTER-ZONE", pt15[0], pt15[1], "Citizen (15km Epicenter)", district));

        // 2. Neighboring district user (20 km away, but DIFFERENT district -> Must be SUPPRESSED_DISTRICT_MISMATCH)
        double[] ptDiff = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 20.0, 60.0);
        list.add(new SubscriberPoint("SUB-20KM-OTHER-DISTRICT", ptDiff[0], ptDiff[1], "Adjacent Sector Citizen (20km Outside Alert District)", district + "-Border-Neighbor"));

        // 3. Inside 100km boundary (95 km) inside alert district
        double[] pt95 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 95.0, 90.0);
        list.add(new SubscriberPoint("SUB-95KM-DISASTER-ZONE", pt95[0], pt95[1], "Field Officer (95km Disaster Zone)", district));

        // 4. Exact 100.0 km boundary -> Disaster + Weather
        double[] pt100 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 100.0, 180.0);
        list.add(new SubscriberPoint("SUB-100KM-BOUNDARY", pt100[0], pt100[1], "Boundary Check (100.0km Disaster+Weather)", district));

        // 5. Exact 100.1 km boundary -> Weather ONLY
        double[] pt100_1 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 100.1, 180.0);
        list.add(new SubscriberPoint("SUB-100.1KM-WEATHER-ONLY", pt100_1[0], pt100_1[1], "Boundary Check (100.1km Weather ONLY)", district));

        // 6. 120 km weather advisory area
        double[] pt120 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 120.0, 270.0);
        list.add(new SubscriberPoint("SUB-120KM-WEATHER-ZONE", pt120[0], pt120[1], "Resident (120km Weather Alert Zone)", district));

        // 7. Exact 150.0 km boundary -> Weather ONLY
        double[] pt150 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 150.0, 315.0);
        list.add(new SubscriberPoint("SUB-150KM-BOUNDARY", pt150[0], pt150[1], "Boundary Check (150.0km Weather ONLY)", district));

        // 8. Exact 150.1 km boundary -> NO ALERT
        double[] pt150_1 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 150.1, 315.0);
        list.add(new SubscriberPoint("SUB-150.1KM-SAFE-ZONE", pt150_1[0], pt150_1[1], "Boundary Check (150.1km Safe / No Alert)", district));

        // 9. Distant safe area (180 km)
        double[] pt180 = com.disaster.agent.geo.HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 180.0, 0.0);
        list.add(new SubscriberPoint("SUB-180KM-SAFE-ZONE", pt180[0], pt180[1], "Remote Sector (180km Safe)", district));

        return list;
    }

    private static class SubscriberPoint {
        String userId;
        double latitude;
        double longitude;
        String name;
        String district;
        SubscriberPoint(String id, double lat, double lon, String name, String district) {
            this.userId = id;
            this.latitude = lat;
            this.longitude = lon;
            this.name = name;
            this.district = district;
        }
    }

    public static class AgentCycleReport {
        public final long cycleNumber;
        public final String locationId;
        public final String city;
        public final double riskScore;
        public final RiskLevel riskLevel;
        public final double confidence;
        public final Trend trend;
        public final RiskLevel predictedRisk;
        public final String engineSource;
        public final List<String> contributingFactors;
        public final String activeAlertId;
        public final int notificationsDispatched;
        public final Instant timestamp;

        public AgentCycleReport(long cycleNumber, String locationId, String city,
                                double riskScore, RiskLevel riskLevel, double confidence,
                                Trend trend, RiskLevel predictedRisk, String engineSource,
                                List<String> contributingFactors, String activeAlertId,
                                int notificationsDispatched, Instant timestamp) {
            this.cycleNumber = cycleNumber;
            this.locationId = locationId;
            this.city = city;
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.confidence = confidence;
            this.trend = trend;
            this.predictedRisk = predictedRisk;
            this.engineSource = engineSource;
            this.contributingFactors = contributingFactors;
            this.activeAlertId = activeAlertId;
            this.notificationsDispatched = notificationsDispatched;
            this.timestamp = timestamp;
        }
    }

    // Getters for agent state
    public Location getLocation() { return location; }
    public RiskAssessment getCurrentAssessment() { return currentAssessment; }
    public WeatherData getCurrentWeather() { return currentWeather; }
    public List<Alert> getActiveAlerts() { return activeAlerts; }
    public List<NotificationResult> getNotificationLog() { return notificationLog; }
    public Map<String, SensorNode> getRegisteredSensors() { return registeredSensors; }
    public SensorGraph getSensorGraph() { return sensorGraph; }
    public long getTotalCyclesExecuted() { return totalCyclesExecuted; }
}
