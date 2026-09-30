package com.disaster.agent.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the multi-criteria risk evaluation fused from sensors, graph correlation, and weather.
 */
public class RiskAssessment {
    private String assessmentId;
    private String locationId;
    private double riskScore; // 0.0 to 100.0
    private RiskLevel riskLevel;
    private double confidence; // 0.0 to 100.0%
    private Trend trend;
    private RiskLevel predictedRisk;
    private String engineSource; // "PYTHON_AI" or "JAVA_FALLBACK_RULES"
    private List<String> contributingFactors = new ArrayList<>();
    private Instant timestamp;

    public RiskAssessment(String assessmentId, String locationId, double riskScore,
                          RiskLevel riskLevel, double confidence, Trend trend,
                          RiskLevel predictedRisk, String engineSource,
                          List<String> contributingFactors, Instant timestamp) {
        this.assessmentId = assessmentId;
        this.locationId = locationId;
        this.riskScore = Math.max(0.0, Math.min(100.0, riskScore));
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.fromScore(this.riskScore);
        this.confidence = Math.max(0.0, Math.min(100.0, confidence));
        this.trend = trend != null ? trend : Trend.STABLE;
        this.predictedRisk = predictedRisk != null ? predictedRisk : this.riskLevel;
        this.engineSource = engineSource != null ? engineSource : "JAVA_FALLBACK_RULES";
        this.contributingFactors = contributingFactors != null ? contributingFactors : new ArrayList<>();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getAssessmentId() { return assessmentId; }
    public String getLocationId() { return locationId; }
    public double getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public double getConfidence() { return confidence; }
    public Trend getTrend() { return trend; }
    public RiskLevel getPredictedRisk() { return predictedRisk; }
    public String getEngineSource() { return engineSource; }
    public List<String> getContributingFactors() { return contributingFactors; }
    public Instant getTimestamp() { return timestamp; }
}
