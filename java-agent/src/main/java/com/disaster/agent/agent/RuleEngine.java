package com.disaster.agent.agent;

import com.disaster.agent.graph.SensorGraph;
import com.disaster.agent.model.*;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic Expert Rule-Based Reasoning Engine (Java OOPJ).
 * Evaluates multi-sensor telemetry, ADSA graph correlations, and weather parameters.
 * Acts as primary fast evaluator and automatic fallback when Python Risk Engine is unreachable.
 */
public class RuleEngine {

    public static RiskAssessment evaluateRisk(String locationId,
                                              Map<SensorType, Double> sensorValues,
                                              SensorGraph.NeighborCorrelationResult neighborCorrelation,
                                              WeatherData weather,
                                              double historicalBaselineAvg) {

        double rainfall = sensorValues.getOrDefault(SensorType.RAINFALL, 0.0);
        double waterLevel = sensorValues.getOrDefault(SensorType.WATER_LEVEL, 0.0);
        double wind = sensorValues.getOrDefault(SensorType.WIND_SPEED, 0.0);
        double humidity = sensorValues.getOrDefault(SensorType.HUMIDITY, 0.0);
        double soilMoisture = sensorValues.getOrDefault(SensorType.SOIL_MOISTURE, 0.0);
        double pressure = sensorValues.getOrDefault(SensorType.PRESSURE, 1013.0);
        double temperature = sensorValues.getOrDefault(SensorType.TEMPERATURE, 28.0);

        List<String> factors = new ArrayList<>();
        double baseScore = 0.0;
        double confidence = 85.0; // Base confidence

        // Incorporate Weather rainfall if physical sensor is low/missing
        if (weather != null && weather.getRainfallMm() > rainfall) {
            rainfall = (rainfall * 0.4) + (weather.getRainfallMm() * 0.6);
            factors.add(String.format("Weather forecast indicates elevated rainfall: %.1f mm", weather.getRainfallMm()));
        }

        // Rule 1: High rainfall + rapidly rising water level + neighboring sensor confirmation = FLOOD RISK
        if (rainfall >= 120.0 && waterLevel >= 4.0) {
            if (neighborCorrelation.isSupportedByNeighbors()) {
                baseScore += 65.0;
                factors.add("Extreme rainfall exceeding critical threshold (>= 120 mm)");
                factors.add("Critical water level rise in drainage channels (>= 4.0 m)");
                factors.add("Neighboring sensors actively confirm spatial hazard cluster (" + neighborCorrelation.getConfirmingCount() + " confirming nodes)");
                confidence += 10.0;
            } else {
                baseScore += 35.0;
                factors.add("Elevated rainfall reading, but unconfirmed by neighboring sensors (possible localized anomaly)");
                confidence -= 25.0;
            }
        } else if (rainfall >= 70.0 || waterLevel >= 3.2) {
            baseScore += 35.0;
            if (rainfall >= 70.0) factors.add("Rainfall above warning threshold (>= 70 mm)");
            if (waterLevel >= 3.2) factors.add("Water level approaching flood danger mark (>= 3.2 m)");
        }

        // Rule 2: Barometric pressure plummet + gale wind speed = CYCLONE / SEVERE STORM
        if (pressure <= 985.0 && wind >= 60.0) {
            baseScore += 30.0;
            factors.add(String.format("Severe barometric depression: %.1f hPa with gale winds: %.1f km/h", pressure, wind));
        } else if (wind >= 50.0) {
            baseScore += 15.0;
            factors.add(String.format("High wind speed advisory: %.1f km/h", wind));
        }

        // Rule 3: High rainfall + soil saturation = LANDSLIDE / COLLAPSE RISK
        if (rainfall >= 80.0 && soilMoisture >= 85.0) {
            baseScore += 20.0;
            factors.add(String.format("Soil moisture critical saturation (%.0f%%) with ongoing rain: Landslide hazard elevated", soilMoisture));
        }

        // Rule 4: Extreme temperature + low humidity + wind = WILDFIRE / HEATWAVE RISK
        if (temperature >= 44.0 && humidity <= 25.0 && wind >= 35.0) {
            baseScore += 35.0;
            factors.add(String.format("Extreme thermal index: %.1f°C with dry conditions (%.0f%% RH): Heatwave / Wildfire advisory", temperature, humidity));
        }

        // Baseline comparison
        Trend trend = Trend.STABLE;
        if (baseScore > 20.0) {
            if (rainfall > historicalBaselineAvg * 1.5) {
                trend = Trend.INCREASING;
                factors.add("Telemetry trajectory shows sharp upward acceleration compared to 24h baseline");
            } else if (rainfall < historicalBaselineAvg * 0.8) {
                trend = Trend.DECREASING;
            }
        }

        if (factors.isEmpty()) {
            baseScore = 12.0;
            factors.add("All sensor telemetry within normal operational baseline boundaries");
        }

        double finalScore = Math.min(100.0, Math.max(5.0, baseScore));
        confidence = Math.min(98.0, Math.max(30.0, confidence * neighborCorrelation.getConfidenceWeight()));

        RiskLevel currentRisk = RiskLevel.fromScore(finalScore);
        RiskLevel predictedRisk = (trend == Trend.INCREASING && finalScore >= 60.0) ?
                RiskLevel.fromScore(Math.min(100.0, finalScore + 15.0)) : currentRisk;

        return new RiskAssessment(
            "RSK-JAVA-" + System.currentTimeMillis(),
            locationId,
            finalScore,
            currentRisk,
            confidence,
            trend,
            predictedRisk,
            "JAVA_FALLBACK_RULES",
            factors,
            Instant.now()
        );
    }
}
