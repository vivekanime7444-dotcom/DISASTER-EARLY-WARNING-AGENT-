package com.disaster.agent.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sensor node representing physical IoT hardware in the ADSA sensor network graph.
 */
public class SensorNode {
    private String sensorId;
    private String zoneId;
    private String locationId;
    private String name;
    private SensorType sensorType;
    private double latitude;
    private double longitude;
    private String unit;
    private double minThreshold;
    private double warningThreshold;
    private double criticalThreshold;
    private double reliabilityScore; // 0.0 to 1.0
    private HealthStatus healthStatus;
    private Instant lastPing;
    private Reading latestReading;
    private List<Reading> recentHistory = new ArrayList<>();

    public SensorNode(String sensorId, String zoneId, String locationId, String name,
                      SensorType sensorType, double latitude, double longitude,
                      String unit, double minThreshold, double warningThreshold,
                      double criticalThreshold, double reliabilityScore) {
        this.sensorId = sensorId;
        this.zoneId = zoneId;
        this.locationId = locationId;
        this.name = name;
        this.sensorType = sensorType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.unit = unit;
        this.minThreshold = minThreshold;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.reliabilityScore = reliabilityScore;
        this.healthStatus = HealthStatus.HEALTHY;
        this.lastPing = Instant.now();
    }

    public void addReading(Reading reading) {
        this.latestReading = reading;
        this.lastPing = reading.getTimestamp();
        this.recentHistory.add(reading);
        if (this.recentHistory.size() > 50) {
            this.recentHistory.remove(0);
        }
    }

    // Getters and Setters
    public String getSensorId() { return sensorId; }
    public String getZoneId() { return zoneId; }
    public String getLocationId() { return locationId; }
    public String getName() { return name; }
    public SensorType getSensorType() { return sensorType; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getUnit() { return unit; }
    public double getMinThreshold() { return minThreshold; }
    public double getWarningThreshold() { return warningThreshold; }
    public double getCriticalThreshold() { return criticalThreshold; }
    public double getReliabilityScore() { return reliabilityScore; }
    public void setReliabilityScore(double reliabilityScore) { this.reliabilityScore = Math.max(0.0, Math.min(1.0, reliabilityScore)); }
    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }
    public Instant getLastPing() { return lastPing; }
    public void setLastPing(Instant lastPing) { this.lastPing = lastPing; }
    public Reading getLatestReading() { return latestReading; }
    public List<Reading> getRecentHistory() { return recentHistory; }
}
