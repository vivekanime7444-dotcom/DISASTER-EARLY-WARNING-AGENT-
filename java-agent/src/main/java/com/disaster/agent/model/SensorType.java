package com.disaster.agent.model;

/**
 * Standard environmental and hazard sensor types supported across all locations.
 */
public enum SensorType {
    RAINFALL("mm", 0.0, 70.0, 120.0),
    WATER_LEVEL("m", 0.5, 3.5, 4.5),
    TEMPERATURE("C", -10.0, 42.0, 48.0),
    HUMIDITY("%", 10.0, 85.0, 95.0),
    WIND_SPEED("km/h", 0.0, 50.0, 80.0),
    WIND_DIRECTION("deg", 0.0, 360.0, 360.0),
    SOIL_MOISTURE("%", 5.0, 75.0, 90.0),
    PRESSURE("hPa", 920.0, 995.0, 980.0); // For pressure, lower indicates severe low pressure / cyclone

    private final String defaultUnit;
    private final double minNormal;
    private final double warningThreshold;
    private final double criticalThreshold;

    SensorType(String defaultUnit, double minNormal, double warningThreshold, double criticalThreshold) {
        this.defaultUnit = defaultUnit;
        this.minNormal = minNormal;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public double getMinNormal() {
        return minNormal;
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }
}
