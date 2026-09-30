package com.disaster.agent.model;

import java.time.Instant;

/**
 * Individual sensor reading telemetry with anomaly and validation flags.
 */
public class Reading {
    private String readingId;
    private String sensorId;
    private double value;
    private String unit;
    private boolean isValid;
    private boolean isAnomaly;
    private ValidationStatus validationStatus;
    private double confidenceWeight; // 0.0 to 1.0
    private Instant timestamp;

    public Reading(String readingId, String sensorId, double value, String unit,
                   boolean isValid, boolean isAnomaly, ValidationStatus validationStatus,
                   double confidenceWeight, Instant timestamp) {
        this.readingId = readingId;
        this.sensorId = sensorId;
        this.value = value;
        this.unit = unit;
        this.isValid = isValid;
        this.isAnomaly = isAnomaly;
        this.validationStatus = validationStatus;
        this.confidenceWeight = confidenceWeight;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public Reading(String sensorId, double value, String unit) {
        this("RDG-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000),
             sensorId, value, unit, true, false, ValidationStatus.VALID, 1.0, Instant.now());
    }

    public String getReadingId() { return readingId; }
    public String getSensorId() { return sensorId; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getUnit() { return unit; }
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }
    public double getConfidenceWeight() { return confidenceWeight; }
    public void setConfidenceWeight(double confidenceWeight) { this.confidenceWeight = confidenceWeight; }
    public Instant getTimestamp() { return timestamp; }
}
