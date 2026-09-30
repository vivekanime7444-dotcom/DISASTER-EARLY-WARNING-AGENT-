package com.disaster.agent.client;

import com.disaster.agent.agent.RuleEngine;
import com.disaster.agent.graph.SensorGraph;
import com.disaster.agent.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Client for communicating with the Python Risk Engine service.
 * Implements resilience, connection timeouts, automatic retry, and seamless failover to Java RuleEngine.
 */
public class PythonRiskClient {
    private static final Logger LOGGER = Logger.getLogger(PythonRiskClient.class.getName());
    private static final String DEFAULT_PYTHON_URL = "http://localhost:8000/api/risk/evaluate";
    
    private final String pythonEndpoint;
    private final HttpClient httpClient;
    private boolean lastPythonStatusHealthy = true;

    public PythonRiskClient(String pythonEndpoint) {
        this.pythonEndpoint = (pythonEndpoint != null && !pythonEndpoint.isBlank()) ? pythonEndpoint : DEFAULT_PYTHON_URL;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public PythonRiskClient() {
        this(DEFAULT_PYTHON_URL);
    }

    /**
     * Sends sensor and weather payload to Python. On connection failure or error,
     * seamlessly executes Java fallback rule evaluation.
     */
    public RiskAssessment evaluateRiskWithFallback(String locationId,
                                                   Map<SensorType, Double> sensorValues,
                                                   SensorGraph.NeighborCorrelationResult neighborCorrelation,
                                                   WeatherData weather,
                                                   double historicalBaselineAvg) {
        try {
            String jsonPayload = buildJsonPayload(locationId, sensorValues, neighborCorrelation, weather);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonEndpoint))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                lastPythonStatusHealthy = true;
                return parsePythonResponse(locationId, response.body());
            } else {
                LOGGER.warning("Python Risk Engine returned HTTP " + response.statusCode() + ". Invoking Java RuleEngine fallback.");
            }
        } catch (Exception ex) {
            if (lastPythonStatusHealthy) {
                LOGGER.info("Python Risk Engine currently offline at " + pythonEndpoint + ". Smoothly activating Java RuleEngine fallback.");
                lastPythonStatusHealthy = false;
            }
        }

        // Resilient Fallback to Java OOPJ Rule Reasoning
        return RuleEngine.evaluateRisk(locationId, sensorValues, neighborCorrelation, weather, historicalBaselineAvg);
    }

    private String buildJsonPayload(String locationId, Map<SensorType, Double> sensorValues,
                                    SensorGraph.NeighborCorrelationResult neighborCorrelation, WeatherData weather) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"location_id\":\"").append(locationId).append("\",");
        sb.append("\"sensors\":{");
        int count = 0;
        for (Map.Entry<SensorType, Double> entry : sensorValues.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey().name().toLowerCase()).append("\":").append(entry.getValue());
        }
        sb.append("},");

        sb.append("\"neighbor_confirmation\":{");
        sb.append("\"supported\":").append(neighborCorrelation.isSupportedByNeighbors()).append(",");
        sb.append("\"confirming_count\":").append(neighborCorrelation.getConfirmingCount()).append(",");
        sb.append("\"total_neighbors\":").append(neighborCorrelation.getTotalNeighbors()).append(",");
        sb.append("\"explanation\":\"").append(escapeJson(neighborCorrelation.getExplanation())).append("\"");
        sb.append("},");

        sb.append("\"weather\":{");
        if (weather != null) {
            sb.append("\"temperature\":").append(weather.getTemperatureC()).append(",");
            sb.append("\"humidity\":").append(weather.getHumidityPct()).append(",");
            sb.append("\"rainfall\":").append(weather.getRainfallMm()).append(",");
            sb.append("\"wind_speed\":").append(weather.getWindSpeedKmh()).append(",");
            sb.append("\"pressure\":").append(weather.getPressureHpa()).append(",");
            sb.append("\"forecast\":\"").append(escapeJson(weather.getForecastSummary() != null ? weather.getForecastSummary() : "Normal")).append("\"");
        } else {
            sb.append("\"rainfall\":0.0");
        }
        sb.append("}}");
        return sb.toString();
    }

    private RiskAssessment parsePythonResponse(String locationId, String jsonBody) {
        // Robust manual JSON token parsing to avoid requiring external libraries
        double riskScore = extractDouble(jsonBody, "risk_score", 0.0);
        String riskLevelStr = extractString(jsonBody, "risk_level", "NORMAL");
        double confidence = extractDouble(jsonBody, "confidence", 80.0);
        String trendStr = extractString(jsonBody, "trend", "STABLE");
        String predictedRiskStr = extractString(jsonBody, "predicted_risk", riskLevelStr);
        List<String> factors = extractStringList(jsonBody, "contributing_factors");

        RiskLevel riskLevel;
        try { riskLevel = RiskLevel.valueOf(riskLevelStr.toUpperCase()); }
        catch (Exception e) { riskLevel = RiskLevel.fromScore(riskScore); }

        Trend trend;
        try { trend = Trend.valueOf(trendStr.toUpperCase()); }
        catch (Exception e) { trend = Trend.STABLE; }

        RiskLevel predictedRisk;
        try { predictedRisk = RiskLevel.valueOf(predictedRiskStr.toUpperCase()); }
        catch (Exception e) { predictedRisk = riskLevel; }

        return new RiskAssessment(
            "RSK-PY-" + System.currentTimeMillis(),
            locationId,
            riskScore,
            riskLevel,
            confidence,
            trend,
            predictedRisk,
            "PYTHON_AI",
            factors,
            Instant.now()
        );
    }

    private double extractDouble(String json, String key, double defaultVal) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern);
            if (idx == -1) return defaultVal;
            int colonIdx = json.indexOf(':', idx);
            if (colonIdx == -1) return defaultVal;
            int endIdx = json.indexOf(',', colonIdx);
            int braceIdx = json.indexOf('}', colonIdx);
            if (endIdx == -1 || (braceIdx != -1 && braceIdx < endIdx)) endIdx = braceIdx;
            String valStr = json.substring(colonIdx + 1, endIdx).replaceAll("[^0-9.-]", "").trim();
            return Double.parseDouble(valStr);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String extractString(String json, String key, String defaultVal) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern);
            if (idx == -1) return defaultVal;
            int colonIdx = json.indexOf(':', idx);
            int firstQuote = json.indexOf('"', colonIdx);
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (firstQuote != -1 && secondQuote != -1) {
                return json.substring(firstQuote + 1, secondQuote);
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private List<String> extractStringList(String json, String key) {
        List<String> result = new ArrayList<>();
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern);
            if (idx == -1) return result;
            int openBracket = json.indexOf('[', idx);
            int closeBracket = json.indexOf(']', openBracket);
            if (openBracket != -1 && closeBracket != -1) {
                String sub = json.substring(openBracket + 1, closeBracket);
                String[] items = sub.split(",");
                for (String it : items) {
                    String clean = it.replaceAll("[\"'\\[\\]]", "").trim();
                    if (!clean.isEmpty()) {
                        result.add(clean);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    public boolean isLastPythonStatusHealthy() {
        return lastPythonStatusHealthy;
    }
}
