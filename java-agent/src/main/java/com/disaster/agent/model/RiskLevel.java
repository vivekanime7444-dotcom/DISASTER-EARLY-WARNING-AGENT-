package com.disaster.agent.model;

/**
 * Prototype risk level categorization based on 0-100 composite risk score.
 * NOTE: These are academic prototype thresholds, not official government disaster standards.
 */
public enum RiskLevel {
    NORMAL(0.0, 20.0, "#10b981"),
    LOW(20.1, 40.0, "#3b82f6"),
    MODERATE(40.1, 60.0, "#f59e0b"),
    HIGH(60.1, 80.0, "#f97316"),
    CRITICAL(80.1, 100.0, "#ef4444");

    private final double minScore;
    private final double maxScore;
    private final String colorHex;

    RiskLevel(double minScore, double maxScore, String colorHex) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.colorHex = colorHex;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static RiskLevel fromScore(double score) {
        if (score <= 20.0) return NORMAL;
        if (score <= 40.0) return LOW;
        if (score <= 60.0) return MODERATE;
        if (score <= 80.0) return HIGH;
        return CRITICAL;
    }
}
