package com.disaster.agent.graph;

/**
 * Weighted edge between two sensor nodes in the ADSA sensor network graph.
 */
public class SensorEdge {
    private String sourceSensorId;
    private String targetSensorId;
    private double distanceKm;
    private double weight; // Coupling strength: 0.0 to 1.0 (higher means stronger physical correlation)
    private String couplingType; // "RIVER_CORRIDOR", "METEOROLOGICAL_PROXIMITY", "SAME_ZONE", "ELEVATION_CONVERGENCE"

    public SensorEdge(String sourceSensorId, String targetSensorId, double distanceKm, double weight, String couplingType) {
        this.sourceSensorId = sourceSensorId;
        this.targetSensorId = targetSensorId;
        this.distanceKm = distanceKm;
        this.weight = Math.max(0.01, Math.min(1.0, weight));
        this.couplingType = couplingType;
    }

    public String getSourceSensorId() { return sourceSensorId; }
    public String getTargetSensorId() { return targetSensorId; }
    public double getDistanceKm() { return distanceKm; }
    public double getWeight() { return weight; }
    public String getCouplingType() { return couplingType; }
}
