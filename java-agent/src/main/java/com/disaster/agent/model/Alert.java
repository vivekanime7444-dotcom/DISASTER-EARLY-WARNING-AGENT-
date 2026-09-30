package com.disaster.agent.model;

import java.time.Instant;
import java.util.List;

/**
 * Emergency warning issued by the Rational Agent when risk criteria exceed safety thresholds.
 */
public class Alert {
    public static final double DISASTER_ALERT_RADIUS_KM = 100.0;
    public static final double WEATHER_ALERT_RADIUS_KM = 150.0;

    private String alertId;
    private String assessmentId;
    private String locationId;
    private String locationName;
    private String alertType; // "DISASTER_ALERT" or "WEATHER_ALERT"
    private String hazardTitle;
    private RiskLevel severity;
    private double riskScore;
    private double rainfallMm;
    private double waterLevelM;
    private double windKmh;
    private double humidityPct;
    private double temperatureC;
    private double confidencePct;
    private Trend trend;
    private RiskLevel predictedRisk;
    private List<String> reasons;
    private double epicenterLat;
    private double epicenterLon;
    private String district;
    private String forecastSummary;
    private String status; // "ACTIVE", "RESOLVED"
    private Instant createdAt;

    public Alert(String alertId, String assessmentId, String locationId, String locationName,
                 String district, String alertType, String hazardTitle, RiskLevel severity, double riskScore,
                 double rainfallMm, double waterLevelM, double windKmh, double humidityPct,
                 double temperatureC, double confidencePct, Trend trend, RiskLevel predictedRisk,
                 List<String> reasons, double epicenterLat, double epicenterLon,
                 String forecastSummary, String status, Instant createdAt) {
        this.alertId = alertId;
        this.assessmentId = assessmentId;
        this.locationId = locationId;
        this.locationName = locationName;
        this.district = district != null ? district : "General District";
        this.alertType = alertType;
        this.hazardTitle = hazardTitle;
        this.severity = severity;
        this.riskScore = riskScore;
        this.rainfallMm = rainfallMm;
        this.waterLevelM = waterLevelM;
        this.windKmh = windKmh;
        this.humidityPct = humidityPct;
        this.temperatureC = temperatureC;
        this.confidencePct = confidencePct;
        this.trend = trend;
        this.predictedRisk = predictedRisk;
        this.reasons = reasons;
        this.epicenterLat = epicenterLat;
        this.epicenterLon = epicenterLon;
        this.forecastSummary = forecastSummary;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**
     * Formats notification text for users inside the 100km Disaster Alert radius.
     */
    public String formatDisasterAlertMessage(double userDistanceKm) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 CRITICAL DISASTER ALERT\n\n");
        sb.append("Location: ").append(locationName).append("\n");
        sb.append("Hazard: ").append(hazardTitle).append("\n");
        sb.append(String.format("Rainfall: %.1f mm\n", rainfallMm));
        sb.append(String.format("Water Level: %.1f m\n", waterLevelM));
        sb.append(String.format("Wind: %.1f km/h\n", windKmh));
        sb.append(String.format("Humidity: %.0f%%\n", humidityPct));
        sb.append(String.format("Risk Score: %.0f/100\n", riskScore));
        sb.append("Risk: ").append(severity).append("\n");
        sb.append(String.format("Confidence: %.0f%%\n", confidencePct));
        sb.append("Trend: ").append(trend).append("\n");
        sb.append("Predicted Risk: ").append(predictedRisk).append("\n\n");
        sb.append("Reasons:\n");
        if (reasons != null) {
            for (String r : reasons) {
                sb.append("• ").append(r).append("\n");
            }
        }
        sb.append("\nYou are within the 100 km disaster-alert area (Distance: ")
          .append(String.format("%.1f km", userDistanceKm)).append(").\n\n");
        sb.append("[VIEW ALERT] [VIEW WEATHER] [DOWNLOAD ALERT]");
        return sb.toString();
    }

    /**
     * Formats notification text for users between 100km and 150km (Weather-Only Alert).
     */
    public String formatWeatherAlertMessage(double userDistanceKm) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌧 WEATHER ALERT\n\n");
        sb.append("Location: ").append(locationName).append("\n");
        sb.append("Weather: ").append(forecastSummary != null ? forecastSummary : "Severe Weather Advisory").append("\n");
        sb.append(String.format("Rainfall: %.1f mm\n", rainfallMm));
        sb.append(String.format("Temperature: %.1f°C\n", temperatureC));
        sb.append(String.format("Humidity: %.0f%%\n", humidityPct));
        sb.append(String.format("Wind: %.1f km/h\n", windKmh));
        sb.append("Forecast: ").append(forecastSummary != null ? forecastSummary : "Heavy conditions expected").append("\n\n");
        sb.append("You are within the 150 km weather-alert area (Distance: ")
          .append(String.format("%.1f km", userDistanceKm)).append(").\n\n");
        sb.append("[VIEW WEATHER] [VIEW FORECAST] [DOWNLOAD REPORT]");
        return sb.toString();
    }

    // Getters
    public String getAlertId() { return alertId; }
    public String getAssessmentId() { return assessmentId; }
    public String getLocationId() { return locationId; }
    public String getLocationName() { return locationName; }
    public String getDistrict() { return district; }
    public String getAlertType() { return alertType; }
    public String getHazardTitle() { return hazardTitle; }
    public RiskLevel getSeverity() { return severity; }
    public double getRiskScore() { return riskScore; }
    public double getRainfallMm() { return rainfallMm; }
    public double getWaterLevelM() { return waterLevelM; }
    public double getWindKmh() { return windKmh; }
    public double getHumidityPct() { return humidityPct; }
    public double getTemperatureC() { return temperatureC; }
    public double getConfidencePct() { return confidencePct; }
    public Trend getTrend() { return trend; }
    public RiskLevel getPredictedRisk() { return predictedRisk; }
    public List<String> getReasons() { return reasons; }
    public double getEpicenterLat() { return epicenterLat; }
    public double getEpicenterLon() { return epicenterLon; }
    public String getForecastSummary() { return forecastSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
